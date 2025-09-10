import java.util.Arrays;

/**
 * 页结构类 - 表示数据库中的一个页面
 * 每个页面大小为4KB，包含页面ID、数据和元数据
 */
public class Page {
    public static final int PAGE_SIZE = 4096; // 4KB页大小
    public static final int INVALID_PAGE_ID = -1; // 无效的页面ID常量

    private int pageId; // 页面ID
    private byte[] data; // 页面数据
    private boolean isDirty; // 页面是否被修改的标志
    private int pinCount; // 页面被pin的计数，表示 某个页面（Page）当前被多少个线程/操作占用。

    /**
     * 构造函数 - 创建新页面
     */
    public Page() {
        this.pageId = INVALID_PAGE_ID; // 初始化页面ID为无效值
        this.data = new byte[PAGE_SIZE]; // 初始化页面数据为4KB大小
        this.isDirty = false; // 初始化页面未被修改
        this.pinCount = 0; // 初始化pin计数为0
        Arrays.fill(data, (byte) 0); // 将页面数据填充为0
    }

    /**
     * 构造函数 - 创建指定ID的页面
     */
    public Page(int pageId) {
        this(); // 调用默认构造函数
        this.pageId = pageId; // 设置页面ID
    }

    // Getter和Setter方法
    public int getPageId() {
        return pageId; // 返回页面ID
    }

    public void setPageId(int pageId) {
        this.pageId = pageId; // 设置页面ID
    }

    public byte[] getData() {
        return data; // 返回页面数据
    }

    public void setData(byte[] data) {
        if (data.length <= PAGE_SIZE) { // 检查数据大小是否超过页面大小
            System.arraycopy(data, 0, this.data, 0, data.length); // 复制数据到页面
            // 如果数据长度小于页面大小，剩余部分保持为0
            if (data.length < PAGE_SIZE) {
                Arrays.fill(this.data, data.length, PAGE_SIZE, (byte) 0); // 填充剩余部分为0
            }
        } else {
            throw new IllegalArgumentException("Data size exceeds page size"); // 抛出异常
        }
    }

    public boolean isDirty() {
        return isDirty; // 返回页面是否被修改
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty; // 设置页面是否被修改
    }

    public int getPinCount() {
        return pinCount; // 返回pin计数
    }

    public void setPinCount(int pinCount) {
        this.pinCount = pinCount; // 设置pin计数
    }

    /**
     * 增加pin计数
     */
    public void pin() {
        this.pinCount++; // pin计数加1
    }

    /**
     * 减少pin计数
     */
    public void unpin() {
        if (this.pinCount > 0) { // 检查pin计数是否大于0
            this.pinCount--; // pin计数减1
        }
    }

    /**
     * 检查页面是否被pin
     */
    public boolean isPinned() {
        return this.pinCount > 0; // 返回pin计数是否大于0
    }

    /**
     * 将字符串写入页面数据
     */
    public void writeString(String str) {
        byte[] strBytes = str.getBytes(); // 将字符串转换为字节数组
        if (strBytes.length < PAGE_SIZE) { // 检查字符串长度是否小于页面大小
            System.arraycopy(strBytes, 0, this.data, 0, strBytes.length); // 写入字符串数据
            // 剩余部分填充0
            Arrays.fill(this.data, strBytes.length, PAGE_SIZE, (byte) 0); // 填充剩余部分为0
            this.isDirty = true; // 设置页面为已修改
        } else {
            throw new IllegalArgumentException("String too long for page"); // 抛出异常
        }
    }

    /**
     * 从页面数据读取字符串
     */
    public String readString() {
        // 找到第一个null字节的位置
        int endIndex = 0;
        while (endIndex < PAGE_SIZE && data[endIndex] != 0) {
            endIndex++; // 找到字符串结束位置
        }
        return new String(data, 0, endIndex); // 返回字符串
    }

    /**
     * 检查页面是否为空
     */
    public boolean isEmpty() {
        for (byte b : data) {
            if (b != 0) { // 检查是否有非0字节
                return false; // 页面不为空
            }
        }
        return true; // 页面为空
    }

    /**
     * 清空页面数据
     */
    public void clear() {
        Arrays.fill(data, (byte) 0); // 将页面数据填充为0
        this.isDirty = true; // 设置页面为已修改
    }

    @Override
    public String toString() {
        return String.format("Page{id=%d, dirty=%s, pinCount=%d, data='%s'}", 
                           pageId, isDirty, pinCount, readString()); // 返回页面信息
    }
}
