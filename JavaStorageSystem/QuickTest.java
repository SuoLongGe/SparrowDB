public class QuickTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== SparrowDB Quick Test ===");
            
            // Initialize storage engine
            System.out.println("1. Initializing storage engine...");
            StorageEngine engine = new StorageEngine(5, "quick_test.db", ReplacementPolicy.LRU);
            System.out.println("   Storage engine ready");
            
            // Allocate a page
            System.out.println("2. Allocating page...");
            int[] pageId = new int[1];
            Page page = engine.allocateNewPage(pageId);
            if (page != null) {
                System.out.println("   Page allocated: " + pageId[0]);
                
                // Write data
                System.out.println("3. Writing data...");
                String testData = "Test data for SparrowDB";
                page.writeString(testData);
                System.out.println("   Data written: " + testData);
                
                // Flush to disk
                System.out.println("4. Flushing to disk...");
                engine.flushPage(pageId[0]);
                System.out.println("   Data flushed");
                
                // Release page
                System.out.println("5. Releasing page...");
                engine.releasePage(pageId[0], false);
                System.out.println("   Page released");
                
                // Read back
                System.out.println("6. Reading back data...");
                Page readPage = engine.getPage(pageId[0]);
                if (readPage != null) {
                    String readData = readPage.readString();
                    System.out.println("   Data read: " + readData);
                    
                    if (testData.equals(readData)) {
                        System.out.println("   SUCCESS: Data matches!");
                    } else {
                        System.out.println("   ERROR: Data mismatch!");
                    }
                    
                    engine.releasePage(pageId[0], false);
                } else {
                    System.out.println("   ERROR: Could not read page");
                }
            } else {
                System.out.println("   ERROR: Could not allocate page");
            }
            
            // Close
            System.out.println("7. Closing storage engine...");
            engine.close();
            System.out.println("   Storage engine closed");
            
            System.out.println("=== Test Complete ===");
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
