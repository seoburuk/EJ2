import org.mindrot.jbcrypt.BCrypt;

public class GenerateHash {
    public static void main(String[] args) {
        String password = "admin1234";
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        System.out.println("Plain: " + password);
        System.out.println("Hash: " + hash);
    }
}
