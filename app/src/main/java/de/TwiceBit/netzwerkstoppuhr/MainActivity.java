package de.TwiceBit.netzwerkstoppuhr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {


    Thread lisen;
    Thread secondUp;
    Handler handler;
    public static boolean running = false;
    public static long seconds = 0;
    Lock lock = new ReentrantLock();
    public static Condition cond;
    public static DatagramSocket socket;
    public static boolean wait = false;
    public static DatagramPacket packet_start;
    public static DatagramPacket packet_stop;
    public static DatagramSocket lisensocket;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.close();
        if(lisensocket != null)
        lisensocket.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cond = lock.newCondition();
        byte[] start = "start".getBytes();
        byte[] stop = "stop".getBytes();

        try {
            packet_start  = new DatagramPacket(start, start.length, InetAddress.getByName("255.255.255.255"), 4444);
            packet_stop  = new DatagramPacket(stop, stop.length, InetAddress.getByName("255.255.255.255"), 4444);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket  = new DatagramSocket(4444);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(running){

                lock.lock();
                synchronized (secondUp){

                       // secondUp.suspend();

                        wait = true;
                        send(packet_stop);


                }

                    running = false;
                    button.setText("Start");

                lock.unlock();

            }else {

                if (secondUp.isAlive()) {

                        send(packet_start);
                        lock.lock();
                        cond.signal();
                        lock.unlock();


                } else {
                    send(packet_start);
                    secondUp.start();
                }
                button.setText("Stop");
                running = true;
            }

            }
        });

    handler = new Handler();

        lisen = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    lisensocket = new DatagramSocket(4444);

                         while(true){
                            byte[] buffer = new byte[255];
                             DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                             lisensocket.receive(packet);
                             if(new String(packet.getData(), 0,5).equalsIgnoreCase("start")){
                                 if(running) continue;
                                 if(secondUp.isAlive()) {
                                     lock.lock();
                                     cond.signal();
                                     lock.unlock();
                                     running = true;
                                 }
                                 else {
                                     secondUp.start();
                                     running = true;
                                 }
                             }else if(new String(packet.getData(), 0,4).equalsIgnoreCase("stop")){
                                 if(!running) continue;
                                if(!secondUp.isAlive()) continue;
                                running = false;
                                wait = true;
                             }

                        }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        lisen.start();

        secondUp = new Thread(new Runnable() {
            @Override
            public void run() {

                final TextView view = findViewById(R.id.Seconds);

                while(true){
                    for (int i = 0; i< 10; i++) {

                        try {
                            if(wait) {
                                synchronized (secondUp) {
                                    lock.lock();
                                    cond.await();
                                    wait = false;
                                    lock.unlock();
                                }
                            }
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //   lock.lock();
                    }
                   /* runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.setText(String.valueOf(++seconds));
                        }
                    });*/
                      handler.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setText(String.valueOf(++seconds));
                            }
                        });

                      //  lock.unlock();
                    }
                }

        });


    }
    public static void send(final DatagramPacket pac){
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket.send(pac);
                    socket.send(pac);
                    socket.send(pac);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
}
