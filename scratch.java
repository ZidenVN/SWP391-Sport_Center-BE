import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class scratch {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
