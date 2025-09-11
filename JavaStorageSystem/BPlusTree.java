import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * B+树索引实现
 */
public class BPlusTree {
    private StorageEngine storageEngine;
    private int rootPageId;
    private int maxKeys;
    private String indexName;
    
    // 常量
    private static final int DEFAULT_MAX_KEYS = 10; // 默认最大键值数量
    
    public BPlusTree(StorageEngine storageEngine, String indexName) {
        this(storageEngine, indexName, DEFAULT_MAX_KEYS);
    }
    
    public BPlusTree(StorageEngine storageEngine, String indexName, int maxKeys) {
        this.storageEngine = storageEngine;
        this.indexName = indexName;
        this.maxKeys = maxKeys;
        this.rootPageId = -1;
        
        // 尝试从存储中加载根页面ID
        loadRootPageId();
    }
    
    /**
     * 插入键值对
     */
    public boolean insert(BPlusTreeKey key, int recordPageId) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        // 如果树为空，创建根节点
        if (rootPageId == -1) {
            return createRoot(key, recordPageId);
        }
        
        // 查找插入位置
        Stack<Integer> path = findPath(key);
        if (path.isEmpty()) {
            return false;
        }
        
        int leafPageId = path.peek();
        BPlusTreeLeafNode leaf = loadLeafNode(leafPageId);
        if (leaf == null) {
            return false;
        }
        
        // 检查键值是否已存在
        if (leaf.containsKey(key)) {
            return false; // 键值已存在
        }
        
        // 插入到叶子节点
        int insertPos = leaf.findKeyPosition(key);
        leaf.insertKey(insertPos, key);
        leaf.insertRecord(insertPos, recordPageId);
        
        // 保存叶子节点
        saveNode(leaf);
        
        // 如果叶子节点溢出，需要分裂
        if (leaf.isFull()) {
            return handleLeafOverflow(leaf, path);
        }
        
        return true;
    }
    
    /**
     * 删除键值
     */
    public boolean delete(BPlusTreeKey key) {
        if (key == null || rootPageId == -1) {
            return false;
        }
        
        // 查找键值位置
        Stack<Integer> path = findPath(key);
        if (path.isEmpty()) {
            return false;
        }
        
        int leafPageId = path.peek();
        BPlusTreeLeafNode leaf = loadLeafNode(leafPageId);
        if (leaf == null) {
            return false;
        }
        
        // 检查键值是否存在
        if (!leaf.containsKey(key)) {
            return false;
        }
        
        // 删除键值
        int deletePos = leaf.findKeyPosition(key);
        leaf.removeKey(deletePos);
        leaf.removeRecord(deletePos);
        
        // 保存叶子节点
        saveNode(leaf);
        
        // 如果叶子节点下溢，需要处理
        if (leaf.isUnderflow() && leaf.getKeyCount() > 0) {
            return handleLeafUnderflow(leaf, path);
        }
        
        return true;
    }
    
    /**
     * 查找键值对应的记录页面ID
     */
    public int search(BPlusTreeKey key) {
        if (key == null || rootPageId == -1) {
            return -1;
        }
        
        // 查找叶子节点
        Stack<Integer> path = findPath(key);
        if (path.isEmpty()) {
            return -1;
        }
        
        int leafPageId = path.peek();
        BPlusTreeLeafNode leaf = loadLeafNode(leafPageId);
        if (leaf == null) {
            return -1;
        }
        
        return leaf.findRecordPageId(key);
    }
    
    /**
     * 范围查询
     */
    public List<Integer> rangeSearch(BPlusTreeKey startKey, BPlusTreeKey endKey) {
        List<Integer> result = new ArrayList<>();
        
        if (startKey == null || endKey == null || rootPageId == -1) {
            return result;
        }
        
        // 找到起始叶子节点
        Stack<Integer> path = findPath(startKey);
        if (path.isEmpty()) {
            return result;
        }
        
        int leafPageId = path.peek();
        BPlusTreeLeafNode leaf = loadLeafNode(leafPageId);
        if (leaf == null) {
            return result;
        }
        
        // 从起始位置开始遍历
        int startPos = leaf.findKeyPosition(startKey);
        while (leaf != null) {
            for (int i = startPos; i < leaf.getKeyCount(); i++) {
                BPlusTreeKey key = leaf.getKey(i);
                if (key.compareTo(endKey) > 0) {
                    return result; // 超出范围
                }
                result.add(leaf.getRecord(i));
            }
            
            // 移动到下一个叶子节点
            leafPageId = leaf.getNextLeafPageId();
            if (leafPageId == -1) {
                break;
            }
            leaf = loadLeafNode(leafPageId);
            startPos = 0;
        }
        
        return result;
    }
    
    /**
     * 获取所有键值
     */
    public List<BPlusTreeKey> getAllKeys() {
        List<BPlusTreeKey> result = new ArrayList<>();
        
        if (rootPageId == -1) {
            return result;
        }
        
        // 找到最左边的叶子节点
        int leafPageId = findLeftmostLeaf();
        BPlusTreeLeafNode leaf = loadLeafNode(leafPageId);
        
        // 遍历所有叶子节点
        while (leaf != null) {
            result.addAll(leaf.getKeys());
            leafPageId = leaf.getNextLeafPageId();
            if (leafPageId == -1) {
                break;
            }
            leaf = loadLeafNode(leafPageId);
        }
        
        return result;
    }
    
    /**
     * 获取树的高度
     */
    public int getHeight() {
        if (rootPageId == -1) {
            return 0;
        }
        
        int height = 1;
        int currentPageId = rootPageId;
        
        while (true) {
            BPlusTreeNode node = loadNode(currentPageId);
            if (node == null || node.isLeaf()) {
                break;
            }
            
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            if (internalNode.getChildCount() == 0) {
                break;
            }
            
            currentPageId = internalNode.getChild(0);
            height++;
        }
        
        return height;
    }
    
    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        if (rootPageId == -1) {
            return 0;
        }
        
        return countNodes(rootPageId);
    }
    
    /**
     * 打印树结构
     */
    public void printTree() {
        if (rootPageId == -1) {
            System.out.println("Empty tree");
            return;
        }
        
        System.out.println("B+ Tree Structure:");
        printNode(rootPageId, 0);
    }
    
    // 私有辅助方法
    
    /**
     * 创建根节点
     */
    private boolean createRoot(BPlusTreeKey key, int recordPageId) {
        int[] newPageId = new int[1];
        Page page = storageEngine.allocateNewPage(newPageId);
        if (page == null) {
            return false;
        }
        
        BPlusTreeLeafNode root = new BPlusTreeLeafNode(newPageId[0], maxKeys);
        root.addKey(key);
        root.addRecord(recordPageId);
        
        saveNode(root);
        rootPageId = newPageId[0];
        saveRootPageId();
        
        return true;
    }
    
    /**
     * 查找从根到叶子的路径
     */
    private Stack<Integer> findPath(BPlusTreeKey key) {
        Stack<Integer> path = new Stack<>();
        
        if (rootPageId == -1) {
            return path;
        }
        
        int currentPageId = rootPageId;
        path.push(currentPageId);
        
        while (true) {
            BPlusTreeNode node = loadNode(currentPageId);
            if (node == null) {
                path.clear();
                return path;
            }
            
            if (node.isLeaf()) {
                break;
            }
            
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            currentPageId = internalNode.findChildPageId(key);
            path.push(currentPageId);
        }
        
        return path;
    }
    
    /**
     * 加载节点
     */
    private BPlusTreeNode loadNode(int pageId) {
        Page page = storageEngine.getPage(pageId);
        if (page == null) {
            return null;
        }
        
        // 读取节点类型
        byte[] data = page.getData();
        boolean isLeaf = (data[4] & 0xFF) != 0; // 第5个字节是isLeaf标志
        
        BPlusTreeNode node;
        if (isLeaf) {
            node = new BPlusTreeLeafNode(pageId, maxKeys);
        } else {
            node = new BPlusTreeInternalNode(pageId, maxKeys);
        }
        
        node.deserializeFromPage(page);
        storageEngine.releasePage(pageId, false);
        
        return node;
    }
    
    /**
     * 加载叶子节点
     */
    private BPlusTreeLeafNode loadLeafNode(int pageId) {
        BPlusTreeNode node = loadNode(pageId);
        return (node != null && node.isLeaf()) ? (BPlusTreeLeafNode) node : null;
    }
    
    /**
     * 加载内部节点
     */
    private BPlusTreeInternalNode loadInternalNode(int pageId) {
        BPlusTreeNode node = loadNode(pageId);
        return (node != null && !node.isLeaf()) ? (BPlusTreeInternalNode) node : null;
    }
    
    /**
     * 保存节点
     */
    private void saveNode(BPlusTreeNode node) {
        Page page = storageEngine.getPage(node.getPageId());
        if (page == null) {
            return;
        }
        
        node.serializeToPage(page);
        storageEngine.releasePage(node.getPageId(), true);
    }
    
    /**
     * 处理叶子节点溢出
     */
    private boolean handleLeafOverflow(BPlusTreeLeafNode leaf, Stack<Integer> path) {
        // 分裂叶子节点
        BPlusTreeLeafNode newLeaf = splitLeafNode(leaf);
        if (newLeaf == null) {
            return false;
        }
        
        // 更新叶子节点链表
        newLeaf.setNextLeafPageId(leaf.getNextLeafPageId());
        newLeaf.setPrevLeafPageId(leaf.getPageId());
        leaf.setNextLeafPageId(newLeaf.getPageId());
        
        if (newLeaf.getNextLeafPageId() != -1) {
            BPlusTreeLeafNode nextLeaf = loadLeafNode(newLeaf.getNextLeafPageId());
            if (nextLeaf != null) {
                nextLeaf.setPrevLeafPageId(newLeaf.getPageId());
                saveNode(nextLeaf);
            }
        }
        
        // 保存分裂后的节点
        saveNode(leaf);
        saveNode(newLeaf);
        
        // 向上传播分裂
        BPlusTreeKey promoteKey = newLeaf.getKey(0);
        return propagateSplit(leaf, newLeaf, promoteKey, path);
    }
    
    /**
     * 分裂叶子节点
     */
    private BPlusTreeLeafNode splitLeafNode(BPlusTreeLeafNode leaf) {
        int[] newPageId = new int[1];
        Page page = storageEngine.allocateNewPage(newPageId);
        if (page == null) {
            return null;
        }
        
        BPlusTreeLeafNode newLeaf = new BPlusTreeLeafNode(newPageId[0], maxKeys);
        newLeaf.setParentPageId(leaf.getParentPageId());
        
        // 移动一半的键值到新节点
        int splitPoint = (maxKeys + 1) / 2;
        for (int i = splitPoint; i < leaf.getKeyCount(); i++) {
            newLeaf.addKey(leaf.getKey(i));
            newLeaf.addRecord(leaf.getRecord(i));
        }
        
        // 从原节点删除移动的键值
        for (int i = leaf.getKeyCount() - 1; i >= splitPoint; i--) {
            leaf.removeKey(i);
            leaf.removeRecord(i);
        }
        
        return newLeaf;
    }
    
    /**
     * 向上传播分裂
     */
    private boolean propagateSplit(BPlusTreeNode leftChild, BPlusTreeNode rightChild, 
                                 BPlusTreeKey promoteKey, Stack<Integer> path) {
        path.pop(); // 移除当前节点
        
        if (path.isEmpty()) {
            // 需要创建新的根节点
            return createNewRoot(leftChild, rightChild, promoteKey);
        }
        
        int parentPageId = path.peek();
        BPlusTreeInternalNode parent = loadInternalNode(parentPageId);
        if (parent == null) {
            return false;
        }
        
        // 在父节点中插入提升的键值
        int insertPos = parent.findKeyPosition(promoteKey);
        parent.insertKey(insertPos, promoteKey);
        parent.insertChild(insertPos + 1, rightChild.getPageId());
        
        // 更新子节点的父页面ID
        rightChild.setParentPageId(parentPageId);
        saveNode(rightChild);
        
        // 保存父节点
        saveNode(parent);
        
        // 如果父节点溢出，继续向上传播
        if (parent.isFull()) {
            return handleInternalOverflow(parent, path);
        }
        
        return true;
    }
    
    /**
     * 创建新的根节点
     */
    private boolean createNewRoot(BPlusTreeNode leftChild, BPlusTreeNode rightChild, 
                                BPlusTreeKey promoteKey) {
        int[] newPageId = new int[1];
        Page page = storageEngine.allocateNewPage(newPageId);
        if (page == null) {
            return false;
        }
        
        BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode(newPageId[0], maxKeys);
        newRoot.addKey(promoteKey);
        newRoot.addChild(leftChild.getPageId());
        newRoot.addChild(rightChild.getPageId());
        
        // 更新子节点的父页面ID
        leftChild.setParentPageId(newPageId[0]);
        rightChild.setParentPageId(newPageId[0]);
        
        saveNode(newRoot);
        saveNode(leftChild);
        saveNode(rightChild);
        
        rootPageId = newPageId[0];
        saveRootPageId();
        
        return true;
    }
    
    /**
     * 处理内部节点溢出
     */
    private boolean handleInternalOverflow(BPlusTreeInternalNode node, Stack<Integer> path) {
        // 分裂内部节点
        BPlusTreeInternalNode newNode = splitInternalNode(node);
        if (newNode == null) {
            return false;
        }
        
        // 保存分裂后的节点
        saveNode(node);
        saveNode(newNode);
        
        // 向上传播分裂
        BPlusTreeKey promoteKey = node.removeKey(node.getKeyCount() - 1);
        return propagateSplit(node, newNode, promoteKey, path);
    }
    
    /**
     * 分裂内部节点
     */
    private BPlusTreeInternalNode splitInternalNode(BPlusTreeInternalNode node) {
        int[] newPageId = new int[1];
        Page page = storageEngine.allocateNewPage(newPageId);
        if (page == null) {
            return null;
        }
        
        BPlusTreeInternalNode newNode = new BPlusTreeInternalNode(newPageId[0], maxKeys);
        newNode.setParentPageId(node.getParentPageId());
        
        // 移动一半的键值和子节点到新节点
        int splitPoint = maxKeys / 2;
        for (int i = splitPoint + 1; i < node.getKeyCount(); i++) {
            newNode.addKey(node.getKey(i));
        }
        
        for (int i = splitPoint + 1; i < node.getChildCount(); i++) {
            int childPageId = node.getChild(i);
            newNode.addChild(childPageId);
            
            // 更新子节点的父页面ID
            BPlusTreeNode child = loadNode(childPageId);
            if (child != null) {
                child.setParentPageId(newPageId[0]);
                saveNode(child);
            }
        }
        
        // 从原节点删除移动的键值
        for (int i = node.getKeyCount() - 1; i >= splitPoint; i--) {
            node.removeKey(i);
        }
        
        // 从原节点删除移动的子节点
        for (int i = node.getChildCount() - 1; i >= splitPoint + 1; i--) {
            node.removeChild(i);
        }
        
        return newNode;
    }
    
    /**
     * 处理叶子节点下溢
     */
    private boolean handleLeafUnderflow(BPlusTreeLeafNode leaf, Stack<Integer> path) {
        // 简化实现：暂时不处理下溢
        // 在实际实现中，需要从兄弟节点借用或合并节点
        return true;
    }
    
    /**
     * 找到最左边的叶子节点
     */
    private int findLeftmostLeaf() {
        if (rootPageId == -1) {
            return -1;
        }
        
        int currentPageId = rootPageId;
        while (true) {
            BPlusTreeNode node = loadNode(currentPageId);
            if (node == null) {
                return -1;
            }
            
            if (node.isLeaf()) {
                return currentPageId;
            }
            
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            if (internalNode.getChildCount() == 0) {
                return -1;
            }
            
            currentPageId = internalNode.getChild(0);
        }
    }
    
    /**
     * 递归计算节点数量
     */
    private int countNodes(int pageId) {
        BPlusTreeNode node = loadNode(pageId);
        if (node == null) {
            return 0;
        }
        
        int count = 1;
        if (!node.isLeaf()) {
            BPlusTreeInternalNode internalNode = (BPlusTreeInternalNode) node;
            for (int i = 0; i < internalNode.getChildCount(); i++) {
                count += countNodes(internalNode.getChild(i));
            }
        }
        
        return count;
    }
    
    /**
     * 递归打印节点
     */
    private void printNode(int pageId, int level) {
        BPlusTreeNode node = loadNode(pageId);
        if (node == null) {
            return;
        }
        
        // 打印缩进
        for (int i = 0; i < level; i++) {
            System.out.print("  ");
        }
        
        // 打印节点信息
        System.out.print("Page " + pageId + ": ");
        if (node.isLeaf()) {
            BPlusTreeLeafNode leaf = (BPlusTreeLeafNode) node;
            System.out.print("Leaf [");
            for (int i = 0; i < leaf.getKeyCount(); i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(leaf.getKey(i));
            }
            System.out.println("]");
        } else {
            BPlusTreeInternalNode internal = (BPlusTreeInternalNode) node;
            System.out.print("Internal [");
            for (int i = 0; i < internal.getKeyCount(); i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(internal.getKey(i));
            }
            System.out.println("]");
            
            // 递归打印子节点
            for (int i = 0; i < internal.getChildCount(); i++) {
                printNode(internal.getChild(i), level + 1);
            }
        }
    }
    
    /**
     * 加载根页面ID
     */
    private void loadRootPageId() {
        // 简化实现：从第一个页面读取根页面ID
        // 在实际实现中，应该有专门的元数据页面
        Page page = storageEngine.getPage(0);
        if (page != null && !page.isEmpty()) {
            byte[] data = page.getData();
            rootPageId = ((data[0] & 0xFF) << 24) |
                        ((data[1] & 0xFF) << 16) |
                        ((data[2] & 0xFF) << 8) |
                        (data[3] & 0xFF);
            storageEngine.releasePage(0, false);
        }
    }
    
    /**
     * 保存根页面ID
     */
    private void saveRootPageId() {
        // 简化实现：保存到第一个页面
        // 在实际实现中，应该有专门的元数据页面
        Page page = storageEngine.getPage(0);
        if (page == null) {
            int[] newPageId = new int[1];
            page = storageEngine.allocateNewPage(newPageId);
            if (page == null) {
                return;
            }
        }
        
        byte[] data = page.getData();
        data[0] = (byte) (rootPageId >>> 24);
        data[1] = (byte) (rootPageId >>> 16);
        data[2] = (byte) (rootPageId >>> 8);
        data[3] = (byte) rootPageId;
        
        page.setData(data);
        storageEngine.releasePage(0, true);
    }
    
    // Getters
    public int getRootPageId() { return rootPageId; }
    public int getMaxKeys() { return maxKeys; }
    public String getIndexName() { return indexName; }
}
