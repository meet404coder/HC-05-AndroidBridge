package meet404coder.hc_05_androidbridge;

/*
        //USING
        BTControl btControl = new BTControl(QrScanningActivity.this);

        btControl.enableBT();

        Map<String,String> Dev = btControl.getPairedBTDevices();
        Dev.keySet(); // Gives all the addresses
        for(String ADDRESS: Dev.keySet()) {
            Dev.get(ADDRESS); //Gives the device name
        }

        btControl.setDevice(AddressOfDevice);

        try {
            btControl.openBTSerial();
        } catch (IOException e1) {

        }
        btControl.beginListenForData(new Runnable() {
            @Override
            public void run() {
                System.out.println(">>>DATA GOT FROM BT");
            }
        });
        //END
 */


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BTControl {

    final private int BT_ENABLE = 1;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    Set<BluetoothDevice> pairedDevices;

    Context mContext;

    public BTControl(Context CallingActivity) {
        this.mContext = CallingActivity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBTEnabled() {
        if (mBluetoothAdapter == null) {
            return false;
        } else if (mBluetoothAdapter.isEnabled()) {
            return true;
        } else {
            return false;
        }
    }

    private int conf = -1;

    public int enableBT() {

        //This function will return values based on the success state of the enableBT function
        /* If BT is not supported -> return value is -1
         * If BT is already enabled -> return 0
         * If BT was not enaled and is now enabled successfully -> returns 1
         * If BT was not enabled and was not successfully enabled  -> returns -1
         */

        if (mBluetoothAdapter == null) {
            return -1;
        } else if (mBluetoothAdapter.isEnabled()) {
            return 0;
        } else {
            try {
                Intent EnableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ((Activity) mContext).startActivityForResult(EnableBluetooth, BT_ENABLE);
                return conf;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    public Map<String, String> getPairedBTDevices() {
        Map<String, String> devicemap = new HashMap<>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return null;
        }

        if (mBluetoothAdapter.isEnabled()) {

            pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice dev : pairedDevices) {
                devicemap.put(dev.getAddress(),dev.getName());
                getUUIDsofPairedDevice(dev.getAddress());
            }
            return devicemap;
        } else {
            return null;
        }
    }

    public UUID[] getUUIDsofPairedDevice(String deviceAddress) {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        UUID uuidsofpaired[];
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equals(deviceAddress)) {
                mmDevice = device;
                try {
                    Method getUUidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                    try {
                        ParcelUuid[] uuids = (ParcelUuid[]) getUUidsMethod.invoke(mBluetoothAdapter, null);
                        int i = 0;
                        uuidsofpaired = new UUID[uuids.length];
                        for (ParcelUuid uuid : uuids) {
                            uuidsofpaired[i] = uuid.getUuid();
                            i++;
                        }
                        return uuidsofpaired;
                    } catch (IllegalAccessException e) {
                        return null;

                    } catch (InvocationTargetException e) {
                        return null;
                    }
                } catch (NoSuchMethodException e) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    public boolean setDevice(String deviceAddress) {
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        UUID uuidsofpaired[];
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equals(deviceAddress)) {
                mmDevice = device;
                try {
                    Method getUUidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                    try {
                        ParcelUuid[] uuids = (ParcelUuid[]) getUUidsMethod.invoke(mBluetoothAdapter, null);
                        int i = 0;
                        uuidsofpaired = new UUID[uuids.length];
                        for (ParcelUuid uuid : uuids) {
                            uuidsofpaired[i] = uuid.getUuid();
                            i++;
                        }
                        return true;
                    } catch (IllegalAccessException e) {
                        return false;

                    } catch (InvocationTargetException e) {
                        return false;
                    }
                } catch (NoSuchMethodException e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }


    public boolean openBTSerial() throws IOException {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void beginListenForData(final Runnable runnable) {
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String BT_DATA_RECEIVED = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    //Data received here
                                    runnable.run();
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }


    public void sendDataOverBT(String msg) throws IOException {
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
    }

    public boolean closeBTSerial() throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
