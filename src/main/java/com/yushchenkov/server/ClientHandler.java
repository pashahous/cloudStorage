package com.yushchenkov.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private String defoultPathToData;

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.defoultPathToData = "server/";
        initLogger();
    }

    @Override
    public void run() {
        try (
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            while (true) {
                String command = in.readUTF();
                if ("upload".equals(command)) {
                    uploading(out, in); // загружаем на сервер
                }
                if ("download".equals(command)) {
                    downloading(out, in); // отправяемт файл из сервера клиенту
                }
                if ("exit".equals(command)) {
                    out.writeUTF("DONE");
                    disconnected();
                    System.out.printf("Client %s disconnected correctly\n", socket.getInetAddress());
                    break;
                }
                System.out.println(command);
                out.writeUTF(command);
            }
        } catch (SocketException socketException) {
            System.out.printf("Client %s disconnected\n", socket.getInetAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLogger() {
        Handler concoleHandler = new ConsoleHandler();
        concoleHandler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getLoggerName() + record.getMessage();
            }
        });
        logger.addHandler(concoleHandler);
        logger.setLevel(Level.ALL);
        logger.fine("Logger in Server loaded");
    }

    private void downloading(DataOutputStream out, DataInputStream in) {
        try {
            File file = new File(defoultPathToData + in.readUTF());

            if (!file.exists()) {
                logger.warning("File " +file.getName()+ "not fount " );
                out.writeUTF("error"); // file not found //1
                return;
            }

            long lenghtFile = file.length();
            out.writeLong(lenghtFile);
            FileInputStream fis = new FileInputStream(file);
            int read;
            byte[] buffer = new byte[8 * 1024];
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void uploading(DataOutputStream out, DataInputStream in) throws IOException {
        try {
            File file = new File("server/" + in.readUTF()); // read file name
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file);
            long size = in.readLong();
            byte[] buffer = new byte[8 * 1024];
            for (int i = 0; i < (size + (8 * 1024 - 1)) / buffer.length; i++) {
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
            }
            fos.close();
            out.writeUTF("OK");
            logger.fine("File " + file + "send to client");
        } catch (Exception e) {
            logger.fine("Error sending file to client ");
            out.writeUTF("WRONG");
            e.printStackTrace();
        }
    }

    private void disconnected() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
