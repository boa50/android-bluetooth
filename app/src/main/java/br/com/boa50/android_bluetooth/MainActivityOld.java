package br.com.boa50.android_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import br.com.boa50.android_bluetooth.R;

public class MainActivityOld extends AppCompatActivity {
    final int REQUEST_ENABLE_BT = 5050;
    final UUID APP_UUID = UUID.fromString("a649f9e9-ce70-4c04-9692-7126573ee50b");

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Vê se o bluetooth é suportado pelo dispositivo
        if (mBluetoothAdapter == null) {
            apresentarToast("Não suporta bluetooth");
        } else {
            //Verifica se o bluetooth está ativo
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else { //Verifica quais dispositivos já estão emparelhados
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice  device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); //MAC address
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    apresentarToast("Bluetooth ativado");
                } else {
                    apresentarToast("Bluetooth não ativado");
                }
                return;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void apresentarToast(String texto) {
        Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }

    private class AcceptBluetoothThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptBluetoothThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("algumNome", APP_UUID);
            } catch (IOException e){
                Log.e("Erro teste", "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e("Erro teste", "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    //Realizar as modificações necessárias em algum outro método passando o socket
                    cancel();
                    break;
                }
            }
        }

        private void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e("Erro teste", "Could not close de connect socket", e);
            }
        }
    }

    private class ConnectBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectBluetoothThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Erro teste", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            mBluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                cancel();
                return;
            }
            //Realizar as modificações necessárias em algum outro método passando o socket
        }

        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Erro teste", "Could not close de connect socket", e);
            }
        }
    }
}

class MyBluetoothService {
    private Handler mHandler;

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
    }

    private class ConncetedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConncetedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e("Erro teste", "Error ocurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("Erro teste", "Error ocurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes;

            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    Message readMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.e("Erro teste", "Input stream was disconnected", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Message writtenMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e("Erro teste", "Error occurred when sending data", e);
                Message writeErrorMsg = mHandler.obtainMessage(MessageConstants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString("toast", "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e("Erro teste", "Could not close the connect socket", e);
            }
        }
    }
}
