package com.tmod.j1;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by gulteking on 10.08.2018 with love.
 */
public class TcpChatServer implements Runnable {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private JTextArea jTextArea;
    private int port;
    private boolean started;
    private boolean clientConnected;
    private static final byte TEXT_MESSAGE = 10;
    private static final byte FILE_TRANSFER_START = 15;
    private static final byte FILE_TRANSFER = 20;


    public TcpChatServer(JTextArea jTextArea, int port) {
        this.jTextArea = jTextArea;
        this.port = port;
    }

    private void sendMessage(String message) {
        if (started && clientConnected) {
            try {
                byte[] messageBytes = message.getBytes();
                byte[] messageWithHeader = new byte[messageBytes.length + 1];
                messageWithHeader[0] = TEXT_MESSAGE;
                System.arraycopy(messageBytes, 0, messageWithHeader, 1, messageBytes.length);
                clientSocket.getOutputStream().write(messageWithHeader);
            } catch (Exception ex) {
                System.err.println("unable to send message. Exception : " + ex + ". Message: " + ex.getMessage());
            }
        } else {
            System.err.println("server not started or client not connected");
        }
    }


    private void sendFile(byte[] fileBytes) {
        if (started && clientConnected) {
            try {
                byte[] fileStartedSignal = new byte[5];
                fileStartedSignal[0] = FILE_TRANSFER_START;
                byte[] fileLengthBytes = ByteBuffer.allocate(4).putInt(fileBytes.length).array();
                System.arraycopy(fileLengthBytes, 0, fileStartedSignal, 1, fileLengthBytes.length);
                clientSocket.getOutputStream().write(fileStartedSignal);


                byte[] fileBytesWithHeader = new byte[fileBytes.length + 1];
                fileBytesWithHeader[0] = FILE_TRANSFER;
                System.arraycopy(fileBytes, 0, fileBytesWithHeader, 1, fileBytes.length);
                clientSocket.getOutputStream().write(fileBytesWithHeader);


            } catch (Exception ex) {
                System.err.println("unable to send file. Exception : " + ex + ". Message: " + ex.getMessage());
            }
        } else {
            System.err.println("server not started");
        }
    }


    private void stop() {
        started = false;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(100000);
            clientSocket = serverSocket.accept();
            clientSocket.setSoTimeout(1000);
            clientConnected = true;

            System.out.println("client connected");


            System.out.println("waiting for messages");

            int bufferLength = 1024;
            while (started) {
                try {
                    byte[] buffer = new byte[bufferLength];
                    int readCount = clientSocket.getInputStream().read(buffer);
                    byte[] receivedData = new byte[readCount];
                    System.arraycopy(buffer, 0, receivedData, 0, receivedData.length);

                    if (receivedData[0] == TEXT_MESSAGE) {
                        String message = new String(receivedData);
                        jTextArea.append("Gelen: " + message + "\n");
                        bufferLength = 1024;
                    } else if (receivedData[0] == FILE_TRANSFER_START) {
                        System.out.println("file transfer start message");
                        bufferLength = ByteBuffer.wrap(receivedData, 1, 4).getInt();
                        System.out.println("file length: " + bufferLength);
                    } else if (receivedData[1] == FILE_TRANSFER) {
                        System.out.println("file message");
                        FileOutputStream faos = new FileOutputStream("file_" + System.currentTimeMillis());
                        faos.write(receivedData, 1, receivedData.length - 1);
                        faos.close();
                        System.out.println("file transfer finished");
                        bufferLength = 1024;
                    } else {
                        System.out.println("unable to parse message");
                        bufferLength = 1024;
                    }

                } catch (Exception ex) {
                    System.out.println("an exception occured while listening messages: " + ex + ". Message: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.err.println("server start exception: " + ex + ". Message: " + ex.getMessage());
        }

        //kapatma kismi
        System.out.println("closing server thread");
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server thread closed");

        clientConnected = false;
        started = false;
        clientSocket = null;
        serverSocket = null;
    }
}
