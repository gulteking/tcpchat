package com.tmod.j1;

import javax.swing.*;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by gulteking on 10.08.2018 with love.
 */
public class TcpChatClient implements Runnable {
    private String remoteAddress;
    private Socket socket;
    private int remotePort;
    private JTextArea jTextArea;
    private boolean started;
    private static final byte TEXT_MESSAGE = 10;
    private static final byte FILE_TRANSFER_START = 15;
    private static final byte FILE_TRANSFER = 20;

    public TcpChatClient(String remoteAddress, int remotePort, JTextArea jTextArea) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.jTextArea = jTextArea;
    }

    private void sendMessage(String message) {
        if (started) {
            try {
                byte[] messageBytes = message.getBytes();
                byte[] messageWithHeader = new byte[messageBytes.length + 1];
                messageWithHeader[0] = TEXT_MESSAGE;
                System.arraycopy(messageBytes, 0, messageWithHeader, 1, messageBytes.length);
                socket.getOutputStream().write(messageWithHeader);
            } catch (Exception ex) {
                System.err.println("unable to send message. Exception : " + ex + ". Message: " + ex.getMessage());
            }
        } else {
            System.err.println("not started");
        }
    }


    private void sendFile(byte[] fileBytes) {
        if (started) {
            try {
                byte[] fileStartedSignal = new byte[5];
                fileStartedSignal[0] = FILE_TRANSFER_START;
                byte[] fileLengthBytes = ByteBuffer.allocate(4).putInt(fileBytes.length).array();
                System.arraycopy(fileLengthBytes, 0, fileStartedSignal, 1, fileLengthBytes.length);
                socket.getOutputStream().write(fileStartedSignal);


                byte[] fileBytesWithHeader = new byte[fileBytes.length + 1];
                fileBytesWithHeader[0] = FILE_TRANSFER;
                System.arraycopy(fileBytes, 0, fileBytesWithHeader, 1, fileBytes.length);
                socket.getOutputStream().write(fileBytesWithHeader);


            } catch (Exception ex) {
                System.err.println("unable to send file. Exception : " + ex + ". Message: " + ex.getMessage());
            }
        } else {
            System.err.println("not start");
        }
    }

    private void stop() {
        started = false;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(remoteAddress, remotePort);
            int bufferLength = 1024;
            while (started) {
                try {
                    byte[] buffer = new byte[bufferLength];
                    int receivedCount = socket.getInputStream().read(buffer);
                    byte[] receivedData = new byte[receivedCount];
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
                    System.err.println("client listener exception: " + ex + ". Message: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            System.err.println("client start exception: " + ex + ". Message: " + ex.getMessage());
        }
    }
}
