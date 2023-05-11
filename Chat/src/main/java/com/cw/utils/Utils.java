package com.cw.utils;

import java.io.Closeable;
import java.io.File;
import java.util.Date;

public class Utils {
    public static void close(Closeable... targets) {
        // Closeable是IO流中接口，"..."可变参数
        // IO流和Socket都实现了Closeable接口，可以直接用
        for (Closeable target: targets) {
            try {
                // 只要是释放资源就要加入空判断
                if (null != target) {
                    target.close();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public  static int calLastedTime(Date lastDate) {
        long now = new Date().getTime();
        long last = lastDate.getTime();
        return (int)((now - last) / 1000);
    }

    public static Date getCurrentime() {
        return new Date();
    }

    public static <T extends Enum<T>> boolean enumContains(Class<T> enumerator, String value)
    {
        for (T c : enumerator.getEnumConstants()) {
            if (c.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static File buildFile(String dataPath, String fileName, Integer index) {
        File file;
        //下标不等于0开始拼后缀
        if (index != 0) {
            int idx = fileName.indexOf('.');
            String suffix = fileName.substring(idx);
            String name = fileName.substring(0, idx);
            file = new File(dataPath + "\\" + name + "(" + index + ")" + suffix);
        } else {
            file = new File(dataPath + "\\" + fileName );
        }
        //判断文件是否存在 文件不存在退出递归
        if (file.isFile()) {
            //每次递归给下标加1
            file = buildFile(dataPath, fileName, ++index);
        }
        return file;
    }
}