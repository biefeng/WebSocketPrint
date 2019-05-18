package com.print;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/*
 *@Author BieFeNg
 *@Date 2019/4/10 10:25
 *@DESC
 */
public class PclPrint {

    private final OutputStream bos;

    private static final int DEFAULT_PORT = 9100;

    Socket socket = null;

    public PclPrint(String ip) {
        this(ip, DEFAULT_PORT);
    }

    public PclPrint(String ip, int port) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, 9100), 1500);
            OutputStream outputStream = socket.getOutputStream();// new FileOutputStream("d:/doc/ggggggggggggggggggggg.pcl");//
            bos = new BufferedOutputStream(outputStream);
        } catch (Exception e) {
            throw new RuntimeException("***************打印机连接失败，IP: " + ip + ", PORT: " + port, e);
        }
    }

    public PclPrint(OutputStream os) {
        bos = os;
    }

    private static final int DEFAULT_BYTE_LEN = 8;


    private String uelS = "1B 25 2D  31 32 33 34 35 58"; //  ESC%-12345X
    private String lf_cr_str = " \r\n";  //LFCR
    private String prj_str = "@PJL "; // @PJL
    private String enter_cmd = "@PJL ENTER LANGUAGE = PCL";
    private String comment_str = "@PJL COMMENT Beginning PCL Job";
    private String reset_cmd = "1B 45";  // ESCE

    private static byte star_b = 42;//字符 *
    private static byte esc_b = 27; //控制字符ESC  十六进制  1b
    private byte l_b = 108; //小写字母l   十六进制  6c
    private byte and_b = 38;//字符 &   十六进制 26
    private byte X_b = 88;//大写字母X   十六进制 4F
    private byte[] uel_b = fromHexStrtoByteArr(uelS);
    private byte[] lf_cr_b = lf_cr_str.getBytes();
    private byte[] reset_cmd_b = fromHexStrtoByteArr(reset_cmd);

    public PclPrint setImageFormate(OutputStream os, ImageSetting settings) throws IOException {
        //Move the cursor to PCL Unit position
        //(300, 400) within the PCL coordinate
        //system.
        os.write(esc_b);
        os.write(star_b);
        os.write(112);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getX()))));
        os.write(120);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getY()))));
        os.write(89);
        // ESC*r#F  Print raster graphics in the orientation of the logical page.
        //  #  the  value of the orientation # = 0,3
        os.write(esc_b);
        os.write(star_b);
        os.write(114);
        os.write(48);
        os.write(70);

        //ESC*t#R set the image dpi
        // # the value of dpi (100,150,200,300,600)

        os.write(esc_b);
        os.write(star_b);
        os.write(116);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getDpi()))));
        os.write(82);

        // ESC*r#S  set the image width
        // # the value of iamge width

        os.write(esc_b);
        os.write(star_b);
        os.write(114);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getWidth()))));
        os.write(83);

        // ESC*r#T  SET THE image height
        // # the value of iamge height

        os.write(esc_b);
        os.write(star_b);
        os.write(114);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getHeight()))));
        os.write(84);

        //Set the left graphics margin to the current
        //X(300) position
        os.write(esc_b);
        os.write(star_b);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getY()))));
        os.write(65);

        //   ESC*b#Y   This specifies a Y offset
        //   # the value of the offset in positive y coordinate #=0-3172


        os.write(esc_b);
        os.write(star_b);
        os.write(98);
        os.write(48);
        os.write(89);

        //   ESC*b#M  Specify the raster compression mode:
        //   # the value of the compression mode

        os.write(esc_b);
        os.write(star_b);
        os.write(98);
        os.write(fromHexStrtoByteArr(stringToAsciiHex(String.valueOf(settings.getOrientation()))));
        os.write(77);

        return this;

    }

    class ImageSetting {
        final int width;
        final int height;
        int dpi = 75;
        int x = 0;
        int y = 0;
        int orientation = 0;

        public ImageSetting(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getDpi() {
            return dpi;
        }

        public void setDpi(int dpi) {
            this.dpi = dpi;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getOrientation() {
            return orientation;
        }

        public void setOrientation(int orientation) {
            this.orientation = orientation;
        }
    }

    public void printSingleImage(BufferedImage image, int copies) throws IOException {
        ImageSetting settings = new ImageSetting(image.getWidth(), image.getHeight());
        settings.setDpi(150);
        init(bos).pageCopy(bos, copies)
                .setImageFormate(bos, settings)
                .handImage(image, bos)
                .end(bos);
    }


    /**
     * 根据设置的值获取图片压缩指令
     *
     * @return
     */
    public static byte[] getRasterBlockCode(int size) {
        String s = stringToAsciiHex(String.valueOf(size));
        String result = "1B 2A 62 " + s + " 57";
        return fromHexStrtoByteArr(result);
    }


    public PclPrint pageCopy(OutputStream bos, int copies) throws IOException {
        bos.write(esc_b);
        bos.write(and_b);
        bos.write(l_b);
        bos.write(copies);
        bos.write(X_b);
        bos.write(lf_cr_b);
        return this;
    }

    /**
     * 可手动设置初始设置
     */
    private PclPrint init(OutputStream bos) throws IOException {
        bos.write(uel_b);
        bos.write(prj_str.getBytes());
        bos.write(lf_cr_b);
        bos.write(comment_str.getBytes());
        bos.write(lf_cr_b);
        bos.write(enter_cmd.getBytes());
        bos.write(lf_cr_b);
        bos.write(reset_cmd_b);
        return this;
    }

    private void end(OutputStream os) throws IOException {
        os.write(reset_cmd_b);
        os.write(uel_b);
        os.flush();
    }

    /**
     * 将单色位图处理成可打印的数据
     */
    public PclPrint handImage(BufferedImage image, OutputStream baos) {
        try {
            WritableRaster raster = image.getRaster();
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = raster.getPixels(0, 0, width, height, new int[width * height]);

            int count_8 = 0;
            int byte_size = width / DEFAULT_BYTE_LEN;
            int mol = width % DEFAULT_BYTE_LEN;
            int rowSize = width;
            if (mol != 0) {
                rowSize = width + DEFAULT_BYTE_LEN;
            }
            byte[] b4w_ = getRasterBlockCode(mol == 0 ? byte_size : byte_size + 1);
            int count_w = 0;
            StringBuilder strBuild = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            outer:
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    int off = h * width + w;
                    if (count_w == 0) {

                        baos.write(b4w_);
                        strBuild.append("b4w");
                    }
                    if (pixels[off] == 0) {
                        sb.append("1");
                    } else {
                        sb.append("0");
                    }

                    if (count_8++ == DEFAULT_BYTE_LEN - 1) {
                        int val = Integer.parseInt(sb.toString(), 2);
                        baos.write(val);
                        sb = new StringBuilder();
                        count_8 = 0;
                        strBuild.append(val + " ");
                    }
                    if (count_w++ == rowSize - 1 && mol == 0) {
                        count_w = 0;
                        continue outer;
                    }
                }
                while (count_w++ < rowSize - 2) {
                    sb.append("0");
                }
                count_8 = 0;
                count_w = 0;
                int val = Integer.parseInt(sb.toString(), 2);
                sb = new StringBuilder();
                strBuild.append(val + " " + "\r\n");
                baos.write(val);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * 将16进制整形字符串转化为字节数组
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
     * 将10进制整形字符串转化为字节数组
     *
     * @param byteArrStr
     * @return
     */
    public static byte[] fromDecStrtoByteArr(String byteArrStr) {
        String[] strs = byteArrStr.trim().replaceAll("[\\r\\n\\t\\s]+", " ").split(" ");
        byte[] bytes = new byte[strs.length];
        int index = 0;
        for (String s : strs) {
            byte b = (byte) (Integer.parseInt(s, 10) & 0xFF);
            bytes[index++] = b;
        }
        return bytes;
    }

    public static String stringToAscii(String value, int radix) {
        StringBuffer sbu = new StringBuffer();
        char[] chars = value.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i != chars.length - 1) {
                sbu.append((int) chars[i]).append(",");
            } else {
                sbu.append((int) chars[i]);
            }
        }
        String s = sbu.toString();
        int val = Integer.parseInt(s);
        if (radix == 16) {
            return Integer.toHexString(val);
        } else if (radix == 2) {
            return Integer.toBinaryString(val);
        } else {
            return s;
        }
    }

    /**
     * 将十进制整数值值转化为ASCII（16进制）
     *
     * @param value
     * @return
     */
    public static String stringToAsciiHex(String value) {
        StringBuffer sb = new StringBuffer();
        for (String s : value.split("")) {
            sb.append(stringToAscii(s, 16) + " ");
        }
        return sb.toString();
    }

    public void closeCon() {
        try {
            if (bos != null) {
                bos.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        PclPrint pclPrint = new PclPrint(new FileOutputStream("d:/doc/test/pcl.prn"));
        BufferedImage image = ImageIO.read(new File("d:/doc/test/1.bmp"));
        pclPrint.printSingleImage(image,51);
    }

}
