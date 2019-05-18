package com.print;

import com.utils.StringUtils;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.imageio.ImageIO;


public class EpsonPrint {

    public static byte FONT_SIZE = 4;// 字体放大一倍
    public static byte FONT_BOLD = 8;// 字体加粗
    public static byte FONT_CLEAR = 0;// 字体加粗

    static int PRINT_PORT = 9100;
    Socket socket = null;
    OutputStream socketOut = null;
    OutputStreamWriter writer = null;
    QRCodeUtil util = null;

    // 建立打印机连接
    public EpsonPrint(String ip, Integer port) {
        try {
            socket = new Socket();
            if (port == null || port == 0) {
                port = PRINT_PORT;
                socket.connect(new InetSocketAddress(ip, PRINT_PORT), 1500);
            } else {
                socket.connect(new InetSocketAddress(ip, port), 1500);
            }
            socketOut = socket.getOutputStream();
            writer = new OutputStreamWriter(socketOut, "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            if (e instanceof UnknownHostException || e instanceof IOException) {
                throw new RuntimeException("********打印机连接失败，IP: " + ip + " Port: " + port + "************");
            }
        }
        this.util = new QRCodeUtil();
    }

    public void setFont(byte type) throws IOException {
        // 清除字体放大指令
        byte[] FONT = new byte[3];
        FONT[0] = 0x1c;
        FONT[1] = 0x21;
        FONT[2] = type;
        socketOut.write(FONT);// 字体放大
    }

    /**
     * 指令换行
     *
     * @throws IOException
     */
    public void directLine() throws IOException {
        socketOut.write(10);
    }

    /**
     * @param str
     * @throws IOException
     */
    public void println(String str) throws IOException {
        if (StringUtils.isNotEmpty(str)) {
            print(str + "\n");
        }
    }

    /**
     * @param str
     * @throws IOException
     */
    public void printFlush(String str) throws IOException {
        println(str);
        writer.flush();
    }

    /**
     * @param str1
     * @param str2
     * @param len
     * @throws IOException
     */
    public void println(String str1, String str2, int len) throws IOException {
        writer.write(alignStr(str1, str2, len) + " \n");
        writer.flush();
    }

    /**
     * @param str
     * @throws IOException
     */
    public void print(String str) throws IOException {
        writer.write(str);
    }

    /**
     * 打印图片
     *
     * @param data
     * @param ins
     * @throws Exception
     */
    public void printImage(String data, InputStream ins) throws Exception {
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        BufferedImage qrImage = qrCodeUtil.createImage(data, ins, true);
        imageAndLR(qrImage, 0x87, 0x00);
    }


    /**
     * 左对齐字符串
     *
     * @param str1
     * @param str2
     * @param lenght 左间距
     * @return
     */
    public String alignStr(String str1, String str2, int lenght) {
        int len1 = str1.length() * 2;
        StringBuffer sb = new StringBuffer();
        sb.append(str1);
        for (; len1++ < lenght; ) {
            sb.append(" ");
        }
        sb.append(str2);
        return sb.toString();
    }


    /**
     * resets all writer settings to default
     */
    public void resetToDefault() throws IOException {
        setInverse(false);
        setBold(false);
        setFontDefault();
        setUnderline(0);
        setJustification(0);
        writer.flush();
    }

    /**
     * @throws IOException
     */
    public void setFontDefault() throws IOException {
        writer.write(0x1c);
        writer.write(0x21);
        writer.write(1);
        writer.flush();
    }

    /**
     * Sets white on black printing
     */
    public void setInverse(Boolean bool) throws IOException {
        writer.write(0x1D);
        writer.write("B");
        writer.write((int) (bool ? 1 : 0));
        writer.flush();
    }

    /**
     * Sets underline and weight
     *
     * @param val 0 = no underline. 1 = single weight underline. 2 = double
     *            weight underline.
     */
    public void setUnderline(int val) throws IOException {
        writer.write(0x1B);
        writer.write("-");
        writer.write(val);
        writer.flush();
    }

    /**
     * Sets left, center, right justification
     *
     * @param val 0 = left justify. 1 = center justify. 2 = right justify.
     */
    public void setJustification(int val) throws IOException {
        writer.write(0x1B);
        writer.write("a");
        writer.write(val);
        writer.flush();
    }

    /**
     * Sets bold
     */
    public void setBold(Boolean bool) throws IOException {
        writer.write(0x1B);
        writer.write("E");
        writer.write((int) (bool ? 1 : 0));
        writer.flush();
    }

    /**
     * @throws IOException
     */
    public void setFontZoomIn() throws IOException {
        /* 横向纵向都放大一倍 */
        writer.write(0x1c);
        writer.write(0x21);
        writer.write(12);
        writer.write(0x1b);
        writer.write(0x21);
        writer.write(12);
    }

    /**
     * Encode and print QR code
     *
     * @param str        String to be encoded in QR.
     * @param errCorrect The degree of error correction. (48 <= n <= 51) 48 = level L /
     *                   7% recovery capacity. 49 = level M / 15% recovery capacity. 50
     *                   = level Q / 25% recovery capacity. 51 = level H / 30% recovery
     *                   capacity.
     * @param moduleSize The size of the QR module (pixel) in dots. The QR code will
     *                   not print if it is too big. Try setting this low and
     *                   experiment in making it larger.
     */
    public void printQR(String str, int errCorrect, int moduleSize)
            throws IOException {

        // save data function 80
        alignQr(1, moduleSize);
        writer.write(0x1D);// init
        writer.write("(k");// adjust height of barcode
        writer.write(str.length() + 3); // pl
        writer.write(0); // ph
        writer.write(49); // cn
        writer.write(80); // fn
        writer.write(48); //
        writer.write(str);

        // error correction function 69
        writer.write(0x1D);
        writer.write("(k");
        writer.write(3); // pl
        writer.write(0); // ph
        writer.write(49); // cn
        writer.write(69); // fn
        writer.write(errCorrect); // 48<= n <= 51

        // size function 67
        writer.write(0x1D);
        writer.write("(k");
        writer.write(3);
        writer.write(0);
        writer.write(49);
        writer.write(67);
        writer.write(moduleSize);// 1<= n <= 16

        // print function 81
        writer.write(0x1D);
        writer.write("(k");
        writer.write(3); // pl
        writer.write(0); // ph
        writer.write(49); // cn
        writer.write(81); // fn
        writer.write(48); // m

        writer.flush();
    }

    /**
     * 二维码排版对齐方式
     *
     * @param position   0：居左(默认) 1：居中 2：居右
     * @param moduleSize 二维码version大小
     * @return
     * @throws IOException
     */
    public void alignQr(int position, int moduleSize) throws IOException {
        writer.write(0x1B);
        writer.write(97);
        if (position == 1) {
            writer.write(1);
            centerQr(moduleSize);
        } else if (position == 2) {
            writer.write(2);
            rightQr(moduleSize);
        } else {
            writer.write(0);
        }
    }

    /**
     * 居中牌排列
     *
     * @param moduleSize 二维码version大小
     * @throws IOException
     */
    public void centerQr(int moduleSize) throws IOException {
        switch (moduleSize) {
            case 1: {
                printSpace(16);
                break;
            }
            case 2: {
                printSpace(18);
                break;
            }
            case 3: {
                printSpace(20);
                break;
            }
            case 4: {
                printSpace(22);
                break;
            }
            case 5: {
                printSpace(24);
                break;
            }
            case 6: {
                printSpace(26);
                break;
            }
            default:
                break;
        }
    }

    /**
     * 二维码居右排列
     *
     * @param moduleSize 二维码version大小
     * @throws IOException
     */
    public void rightQr(int moduleSize) throws IOException {
        switch (moduleSize) {
            case 1:
                printSpace(14);
                break;
            case 2:
                printSpace(17);
                break;
            case 3:
                printSpace(20);
                break;
            case 4:
                printSpace(23);
                break;
            case 5:
                printSpace(26);
                break;
            case 6:
                printSpace(28);
                break;
            default:
                break;
        }
    }

    /**
     * 打印空白
     *
     * @param length 需要打印空白的长度
     * @throws IOException
     */
    public void printSpace(int length) throws IOException {
        for (int i = 0; i < length; i++) {
            writer.write(" ");
        }
        writer.flush();
    }

    /**
     * 初始化打印机
     *
     * @return
     * @throws IOException
     */
    public void init() throws IOException {
        writer.write(0x1B);
        writer.write(0x40);
    }

    /**
     * 打印图片并换行
     * 由nl和nh共同确定打印的水平位置 0<nl(byte)<255和0=<nh<=2的值会被转为byte，如果nl为16或10进制，超过byte范围则会进行取余
     *
     * @param image
     * @param positionNl
     * @param positionNh
     * @throws Exception
     */
    public void imageAndLR(BufferedImage image, int positionNl, int positionNh) throws Exception {
        byte[] bytes = ImagePixelUtil.imagePixelToPosByte_24(image, 33, positionNl, positionNh);
        write(new byte[]{27, 51, 15});//设置行间距
        write(bytes);
    }

    public void image(BufferedImage image, int positionNl, int positionNh) throws Exception {
        byte[] bytes = ImagePixelUtil.imagePixelToPosByte_24(image, 33, positionNl, positionNh);
        bytes = Arrays.copyOfRange(bytes, 0, bytes.length - 5);
        write(new byte[]{27, 51, 15});//设置行间距
        write(bytes);
    }

    public void write(byte... data) throws IOException {
        socketOut.write(data);
        socketOut.flush();
    }

    /**
     * To control the printer performing paper feed and cut paper finally
     *
     * @throws IOException
     */
    public void feedAndCut() throws IOException {
        feed();
        cut();
        writer.flush();
    }

    /**
     * To control the printer performing paper feed
     *
     * @throws IOException
     */
    public void feed() throws IOException {
        // 下面指令为打印完成后自动走纸
        writer.write(27);
        writer.write(100);
        writer.write(4);
        writer.write(10);

        writer.flush();

    }

    /**
     * Cut paper, functionality whether work depends on printer hardware
     *
     * @throws IOException
     */
    public void cut() throws IOException {
        // cut
        writer.write(0x1D);
        writer.write("V");
        writer.write(48);
        writer.write(0);

        writer.flush();
    }

    /**
     * at the end, close writer and socketOut
     */
    public void closeConn() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (socketOut != null) {
                socketOut.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
