/**
 * B+ Tree Key Interface - Defines key comparison and serialization operations
 */
public interface BPlusTreeKey extends Comparable<BPlusTreeKey> {
    /**
     * Serialize key to byte array
     */
    byte[] toBytes();
    
    /**
     * Deserialize key from byte array
     */
    void fromBytes(byte[] data, int offset);
    
    /**
     * Get the number of bytes occupied by the key
     */
    int getSize();
    
    /**
     * Get string representation of the key
     */
    String toString();
}

/**
 * Integer Key Implementation
 */
class IntegerKey implements BPlusTreeKey {
    private int value;
    
    public IntegerKey(int value) {
        this.value = value;
    }
    
    public IntegerKey() {
        this.value = 0;
    }
    
    public int getValue() {
        return value;
    }
    
    @Override
    public int compareTo(BPlusTreeKey other) {
        if (other instanceof IntegerKey) {
            return Integer.compare(this.value, ((IntegerKey) other).value);
        }
        throw new IllegalArgumentException("Cannot compare IntegerKey with " + other.getClass().getSimpleName());
    }
    
    @Override
    public byte[] toBytes() {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (value >>> 24);
        bytes[1] = (byte) (value >>> 16);
        bytes[2] = (byte) (value >>> 8);
        bytes[3] = (byte) value;
        return bytes;
    }
    
    @Override
    public void fromBytes(byte[] data, int offset) {
        this.value = ((data[offset] & 0xFF) << 24) |
                    ((data[offset + 1] & 0xFF) << 16) |
                    ((data[offset + 2] & 0xFF) << 8) |
                    (data[offset + 3] & 0xFF);
    }
    
    @Override
    public int getSize() {
        return 4;
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IntegerKey that = (IntegerKey) obj;
        return value == that.value;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}

/**
 * String Key Implementation
 */
class StringKey implements BPlusTreeKey {
    private String value;
    private static final int MAX_LENGTH = 255; // Maximum string length
    
    public StringKey(String value) {
        this.value = value != null ? value : "";
        if (this.value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("String too long: " + this.value.length());
        }
    }
    
    public StringKey() {
        this.value = "";
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public int compareTo(BPlusTreeKey other) {
        if (other instanceof StringKey) {
            return this.value.compareTo(((StringKey) other).value);
        }
        throw new IllegalArgumentException("Cannot compare StringKey with " + other.getClass().getSimpleName());
    }
    
    @Override
    public byte[] toBytes() {
        byte[] strBytes = value.getBytes();
        byte[] result = new byte[1 + strBytes.length]; // 1 byte for length + string bytes
        result[0] = (byte) strBytes.length;
        System.arraycopy(strBytes, 0, result, 1, strBytes.length);
        return result;
    }
    
    @Override
    public void fromBytes(byte[] data, int offset) {
        int length = data[offset] & 0xFF;
        if (length > 0) {
            this.value = new String(data, offset + 1, length);
        } else {
            this.value = "";
        }
    }
    
    @Override
    public int getSize() {
        return 1 + value.getBytes().length; // 1 byte for length + string bytes
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StringKey stringKey = (StringKey) obj;
        return value.equals(stringKey.value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}