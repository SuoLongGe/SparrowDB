import java.util.Arrays;

/**
 * 页结构类 - 表示数据库中的一个页面
 * 每个页面大小为4KB，包含页面ID、数据和元数据
 */
public class Page {
    public static final int PAGE_SIZE = 4096; // 4KB页大小
    public static final int INVALID_PAGE_ID = -1;
    
    private int pageId;
    private byte[] data;
    private boolean isDirty;
    private int pinCount;
    
    /**
     * 构造函数 - 创建新页面
     */
    public Page() {
        this.pageId = INVALID_PAGE_ID;
        this.data = new byte[PAGE_SIZE];
        this.isDirty = false;
        this.pinCount = 0;
        Arrays.fill(data, (byte) 0);
    }
    
    /**
     * 构造函数 - 创建指定ID的页面
     */
    public Page(int pageId) {
        this();
        this.pageId = pageId;
    }
    
    // Getter和Setter方法
    public int getPageId() {
        return pageId;
    }
    
    public void setPageId(int pageId) {
        this.pageId = pageId;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        if (data.length <= PAGE_SIZE) {
            System.arraycopy(data, 0, this.data, 0, data.length);
            // 如果数据长度小于页面大小，剩余部分保持为0
            if (data.length < PAGE_SIZE) {
                Arrays.fill(this.data, data.length, PAGE_SIZE, (byte) 0);
            }
        } else {
            throw new IllegalArgumentException("Data size exceeds page size");
        }
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }
    
    public int getPinCount() {
        return pinCount;
    }
    
    public void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }
    
    /**
     * 增加pin计数
     */
    public void pin() {
        this.pinCount++;
    }
    
    /**
     * 减少pin计数
     */
    public void unpin() {
        if (this.pinCount > 0) {
            this.pinCount--;
        }
    }
    
    /**
     * 检查页面是否被pin
     */
    public boolean isPinned() {
        return this.pinCount > 0;
    }
    
    /**
     * 将字符串写入页面数据
     */
    public void writeString(String str) {
        byte[] strBytes = str.getBytes();
        if (strBytes.length < PAGE_SIZE) {
            System.arraycopy(strBytes, 0, this.data, 0, strBytes.length);
            // 剩余部分填充0
            Arrays.fill(this.data, strBytes.length, PAGE_SIZE, (byte) 0);
            this.isDirty = true;
        } else {
            throw new IllegalArgumentException("String too long for page");
        }
    }
    
    /**
     * 从页面数据读取字符串
     */
    public String readString() {
        // 找到第一个null字节的位置
        int endIndex = 0;
        while (endIndex < PAGE_SIZE && data[endIndex] != 0) {
            endIndex++;
        }
        return new String(data, 0, endIndex);
    }
    
    /**
     * 检查页面是否为空
     */
    public boolean isEmpty() {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 清空页面数据
     */
    public void clear() {
        Arrays.fill(data, (byte) 0);
        this.isDirty = true;
    }
    
    @Override
    public String toString() {
        return String.format("Page{id=%d, dirty=%s, pinCount=%d, data='%s'}", 
                           pageId, isDirty, pinCount, readString());
    }
}
