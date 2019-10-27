package com.example.dsd6;

import android.os.Handler;
import android.util.Log;

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
    int type = 0;
    int dataCnt = 0;
    int block = -1;
    private int[] retrievedBlocks = new int[40];
    private int totalRetrievedBlocks = 0;
    DbHandler mDbHandler;
    ProcessorCallbacks mCallbacks;

    Handler hndBlock = new Handler();

    public BlockProcessor(DbHandler dbHandler, ProcessorCallbacks callbacks) {
        mCallbacks = callbacks;
        mDbHandler = dbHandler;
    }

    public void init() {
        totalRetrievedBlocks = 0;
        for (int i = 0; i < retrievedBlocks.length; i++)
            retrievedBlocks[i] = 0;
    }

    public void getBlocks() {
        for (int i = 0; i < retrievedBlocks.length; i++)
            if (retrievedBlocks[i]==0)
                mCallbacks.send("AT+DATA=" + i);
    }

    private void markBlockAsDownloaded(int block)
    {
        //Log.i("caca","block "+ block + " "+ retrievedBlocks[block]+"\n");

        if (block>=0 && block< retrievedBlocks.length) {
            int progress = 0;
            synchronized (retrievedBlocks) {
                if (retrievedBlocks[block] != 0) {
                    return;
                }
                retrievedBlocks[block] = 1;
                totalRetrievedBlocks++;
                progress = (totalRetrievedBlocks * 100) / retrievedBlocks.length;
            }
            mCallbacks.setProgress(progress);
        }
        else {
            mCallbacks.addText("err: Unknown block:" + block);
        }
    }


    public void process(byte[] s)
    {
        //Log.i("process",new String(s)+"\n");

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
                    if (s[i]=='\n') {
                        Log.i("command",""+ command );
                        if (command.startsWith("DATA")) {
                            String[] params = command.split(":");
                            String[] ss = params[1].split(",");
                            type = Integer.valueOf(ss[0]);
                            len = Integer.valueOf(ss[1]);
                            block = Integer.valueOf(ss[3]);
                            data = new byte[len];
                            dataCnt = 0;
                            state++;
                            if (len==0) {
                                //block with no data
                                markBlockAsDownloaded(block);
                                state = 0;
                            }
                        }
                        else {
                            state=0;
                        }
                    }
                    else {
                        if (s[i] != '\r')
                            command += (char)s[i];
                    }
                    break;
                case 4: {
                    data[dataCnt++] = s[i];
                    if (dataCnt==len)
                    {
                        for(int o=0;o<len;) {

                            int flags = (data[o + 0] & 0xff) >>6;

                            long timestamp = 0;
                            timestamp = timestamp * 256 + ((data[o++]& 0xff) & 0x3f);
                            timestamp = timestamp * 256 + (data[o++]& 0xff);
                            timestamp = timestamp * 256 + (data[o++]& 0xff);
                            timestamp = timestamp * 256 + (data[o++]& 0xff);
                            timestamp += 1262304000;

                            int value = 0;
                            value = value * 256 + (data[o++]& 0xff);
                            value = value * 256 + (data[o++]& 0xff);

                            int value2 = 0;
                            if (type==1) {
                                //realtime data
                                value2 = value2 * 256 + (data[o++]& 0xff);
                                value2 = value2 * 256 + (data[o++]& 0xff);
                            }

                            try {
                                mDbHandler.insertData(type, flags, timestamp, value, value2);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }

                            //String timestampStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timestamp * 1000));
                            //AddText((o/6) + ": " + timestampStr + " "+ value+"\n");
                        }
                        state=0;

                        markBlockAsDownloaded(block);
                    }
                    break;
                }
                default:
                {
                    Log.i("caca","block "+ block + " "+ retrievedBlocks[block]+"\n");
                }
            }
        }
    }
}
