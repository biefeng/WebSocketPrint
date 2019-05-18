package com.print;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 *@Author BieFeNg
 *@Date 2019/4/17 18:22
 *@DESC
 */
public class DirectProtocolPrint {


    /*
     *           x coordinate
     *           ^
     *     100   |
     *           |
     *           |
     *     40    |        * (40,40)
     *           |
     *           |--------------------->   y coordinate
     *        (0,0)       40           100
     *
     * */


    private Socket socket;
    private ByteArrayOutputStream baos;
    private String fontSetting = "FONTD \"MHeiGB18030C-Medium\",8,20,100\r\n";   //中文字体
    private static final int DEFAULT_PORT = 9100;
    InetSocketAddress socketAddress = null;

    public static class FontName {
        public static final String UNIVERS = "Univers";
        public static final String UNIVERS_BOLD = "Univers Bold";
        public static final String MHEI_108030 = "MHeiGB18030C-Medium";
    }

    public DirectProtocolPrint(String ip) {
        this(ip, DEFAULT_PORT);
    }

    public DirectProtocolPrint(String ip, int port) {
        this.socketAddress = new InetSocketAddress(ip, port);
        this.socket = new Socket();
        try {
            socket.connect(socketAddress, 1500);
            baos = new ByteArrayOutputStream();
            setEncoding(); // 默认utf8
        } catch (IOException e) {
            throw new RuntimeException("********HoneyWell打印机连接失败，IP: " + ip + " Port: " + port + "************");
        } catch (Exception e) {
            if (e instanceof UnknownHostException) {
                throw new RuntimeException("********打印机连接失败，找不到host，IP: " + ip + " Port: " + port + "************");
            }
        }
    }

    public void setEncoding() throws IOException {
        baos.write("CLL\r\n".getBytes()); //清楚图片缓冲区
        baos.write("NASC 8\r\n".getBytes());  //设置编码utf-8
    }

    public void setFont(String fontName, int fontSize) {
        fontSetting = "FONTD \"" + fontName + "\"," + fontSize + ",1,100\r\n";
    }

    public void printText(int x, int y, String text) throws IOException {
        StringBuffer sb = new StringBuffer(position(x, y)).append(fontSetting).append("PT \"" + text + "\"").append("\r\n");
        baos.write(sb.toString().getBytes());
    }

    public void printQRCode(int x, int y, String text, int height) throws IOException {
        StringBuffer sb = new StringBuffer(position(x, y))
                .append("BARSET \"QRCODE\",").append("").append("1,1,").append(height).append(",2,2\r\n")//BARSET "QRCODE",1,1,3,2,2
                .append("PB ").append("\"").append(text).append("\"\r\n"); //PB "Hello worldW"
        baos.write(sb.toString().getBytes());

    }

    public void printImage(int x, int y, InputStream ins) throws IOException {
        // baos
        //.write(position(x, y).getBytes());
        baos.write(getImageData(ins));
        baos.write("\r\n".getBytes());

    }

    /**
     * 打印图片（支持单色位图，其它色位图片可通过WINDOWS画图工具另存为单色位图）
     *
     * @param x
     * @param y
     * @param image
     */
    public void printImage(int x, int y, BufferedImage image) {
        try {
            byte[] data = rllFormat(image);
            StringBuffer sb = new StringBuffer();
            baos.write(sb.append("pp").append(x).append(",").append(y).append(":")
                    .append("PRBUF " + (data.length + 6))  //设置打印数据长度（包括前6个字节（40,02两个加上宽（2位），高（2位）））
                    .append("\r\n").toString().getBytes());
            baos.write(fromHexStrtoByteArr("40 02"));  // @2
            baos.write(fromHexStrtoByteArr(fromDecToHex(image.getWidth())));
            baos.write(fromHexStrtoByteArr(fromDecToHex(image.getHeight())));
            baos.write(data);
            baos.write("\r\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 将16进制(ASCII)整形字符串转化为字节数组
     * 示例："48 65 6C 6C 6F"   =>   [72,101,108,108,111]  = "Hello"
     *
     * @param byteArrStr
     * @return
     */
    public static byte[] fromHexStrtoByteArr(String byteArrStr) {
        String[] strs = byteArrStr.trim().replaceAll("[\\r\\n\\t\\s]+", " ").split(" ");
        byte[] bytes = new byte[strs.length];
        int index = 0;
        for (String s : strs) {
            byte b = (byte) (Integer.parseInt(s, 16) & 0xFF);
            bytes[index++] = b;
        }
        return bytes;
    }

    /**
     * 将10进制转为2个16进制值表示，并拼接为字符串,以空格隔开
     * 示例： 256*2 + 43 = 555  =>  "02 2B"
     *
     * @param value
     */
    public static String fromDecToHex(int value) {
        StringBuffer sb = new StringBuffer();
        for (int i = 3; i >= 0; i--) {
            int cardinality = 1 << (4 * i);
            String hex = "0";
            if (i != 0) {
                int var = value / cardinality;
                hex = Integer.toHexString(var);
                value = value % cardinality;
            } else {
                hex = Integer.toHexString(value);

            }
            if (i == 1) {
                sb.append(" ");
            }
            sb.append(hex);

        }
        return sb.toString();
    }

    //将单色位图用RLL算法进行压缩
    public byte[] rllFormat(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableRaster raster = image.getRaster();
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = raster.getPixels(0, 0, width, height, new int[width * height]);

        for (int h = 0; h < height; h++) {
            int count = 0;
            boolean flag = false;  // 写入标记，当由白点变为黑点或黑点变为白点时，将统计到的连续的点数写入
            for (int w = 0; w < width; w++) {

                boolean changed = false;
                int offset = h * width + w;
                int pixel = pixels[offset];
                if (offset > 0) {
                    changed = pixel != pixels[offset - 1];
                } else {
                    changed = true;
                }
                if (w == 0) {
                    if (pixel != 0) {
                        count += 1;
                        continue;
                    } else {
                        flag = true;
                    }
                }

                if (changed || w == width - 1) {
                    flag = true;
                    if (w == width - 1) {
                        count += 1;
                    }
                } else if (!changed) {
                    count += 1;
                }
                if (flag) {
                    int div = count / 255;
                    int mol = count % 255;
                    for (int i = 0; i < div; i++) {
                        baos.write(255);
                        baos.write(0);
                    }
                    baos.write(mol);
                    if (w == width - 1 && pixel == 0) {
                        baos.write(0x00);
                    }
                    if (changed) {
                        count = 1;
                    } else {
                        count = 0;
                    }
                    flag = false;
                }
            }
        }
        return baos.toByteArray();
    }

    public String position(int x, int y) {
        StringBuffer sb = new StringBuffer();
        sb.append("pp").append(x).append(",").append(y).append("\r\n");
        return sb.toString();
    }

    public void doPrint() throws IOException {
        baos.write("PF\r\nPRINT KEY OFF\r\n".getBytes());
        if (null == socket || socket.isClosed()) {
            socket = new Socket();
            socket.connect(socketAddress);
        }
        OutputStream os = socket.getOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(os);
        bos.write(baos.toByteArray());
        bos.flush();
        bos.close();
        socket.close();
    }

    public byte[] getImageData(InputStream ins) throws IOException {

        int len = 0;
        byte[] bytes = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((len = ins.read(bytes)) != -1) {
            baos.write(bytes, 0, len);
        }
        return baos.toByteArray();
    }

}
