public class SimpleDebug {
    public static void main(String[] args) {
        System.out.println("Program started");
        System.out.flush();
        
        try {
            System.out.println("About to create DatabaseEngine");
            System.out.flush();
            
            // Just try to load the class
            Class<?> engineClass = Class.forName("com.database.engine.DatabaseEngine");
            System.out.println("DatabaseEngine class loaded successfully: " + engineClass.getName());
            System.out.flush();
            
            // Try to create an instance
            Object engine = engineClass.getConstructor(String.class, String.class)
                .newInstance("test_db", "./test_data");
            System.out.println("DatabaseEngine instance created: " + engine);
            System.out.flush();
            
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.out.flush();
        }
        
        System.out.println("Program completed");
        System.out.flush();
    }
}
