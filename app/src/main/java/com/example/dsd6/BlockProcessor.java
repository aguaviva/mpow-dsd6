package com.example.dsd6;

import android.os.Handler;

public class BlockProcessor {

    public interface ProcessorCallbacks
    {
        void setProgress(final int i);
        void addText(final String str);
        void send(String command);
    }

    int state = 0;
    String command;
    int len = 0;
    byte[] data;
    int dataCnt = 0;
    int block = -1;
    private int[] retrievedBlocks = new int[30];
    private int totalRetrievedBlocks = 0;
    DbHandler mDbHandler;
    ProcessorCallbacks mCallbacks;

    Handler hndBlock = new Handler();

    public BlockProcessor() {
        for(int i = 0; i< retrievedBlocks.length; i++)
        {
            retrievedBlocks[i]=0;
        }
    }

    void getBlocks(DbHandler dbHandler, ProcessorCallbacks callbacks) {
        mCallbacks = callbacks;
        mDbHandler = dbHandler;
        totalRetrievedBlocks = 0;
        Runnable rr = new Runnable() {
            public void run() {
                int requested = 0;
                for (int i = 0; i < retrievedBlocks.length; i++) {
                    if (retrievedBlocks[i] == 0) {
                        mCallbacks.send("AT+DATA=" + i);
                        requested++;

                        // send bursts of max 5 requests
                        if (requested>5)
                            break;
                    }
                }

                if (requested>0)
                    hndBlock.postDelayed(this,1000);
            }
        };
        hndBlock.post(rr);
    }

    public void markBlockAsDownloaded(int block)
    {
        if (block>=0 && block< retrievedBlocks.length) {
            retrievedBlocks[block] = 1;
            totalRetrievedBlocks++;
            int progress = (totalRetrievedBlocks*100)/retrievedBlocks.length;
            mCallbacks.setProgress(progress);
        }
        else {
            mCallbacks.addText("err: Unknown block:" + block);
        }
    }


    public void process(byte[] s)
    {
        for (int i = 0; i < s.length; i++) {
            switch (state) {
                case 0:
                    if (s[i] == 'A') state++;
                    else state = 0;
                    break;
                case 1:
                    if (s[i] == 'T') state++;
                    else state = 0;
                    break;
                case 2:
                    if (s[i] == '+') {
                        state++;
                        command = "";
                    } else state = 0;
                    break;
                case 3:
                    if (s[i] == '\n') {
                        if (command.startsWith("DATA")) {
                            String[] ss = command.split(",");
                            len = Integer.valueOf(ss[1]);
                            block = Integer.valueOf(ss[3]);
                            data = new byte[len];
                            dataCnt = 0;
                            state++;
                            if (len==0) {
                                markBlockAsDownloaded(block);
                                state = 0;
                            }
                        }
                        else {
                            state=0;
                        }

                        //AddText("* " + command + "\n");
                    } else {
                        if (s[i] != '\r')
                            command += (char)s[i];
                    }
                    break;
                case 4: {
                    data[dataCnt++] = s[i];
                    if (dataCnt==len)
                    {
                        for(int o=0;o<len;o+=6) {

                            int type = (data[o + 0] & 0xff) >>6;

                            long timestamp = 0;
                            timestamp = timestamp * 256 + ((data[o + 0]& 0xff) & 0x3f);
                            timestamp = timestamp * 256 + (data[o + 1]& 0xff);
                            timestamp = timestamp * 256 + (data[o + 2]& 0xff);
                            timestamp = timestamp * 256 + (data[o + 3]& 0xff);
                            timestamp += 1262304000;

                            int value = 0;
                            value = value * 256 + (data[o + 4]& 0xff);
                            value = value * 256 + (data[o + 5]& 0xff);

                            mDbHandler.insertData(type, timestamp, value);

                            //String timestampStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timestamp * 1000));
                            //AddText((o/6) + ": " + timestampStr + " "+ value+"\n");
                        }
                        state=0;

                        markBlockAsDownloaded(block);
                    }
                }
            }
        }
    }
}
