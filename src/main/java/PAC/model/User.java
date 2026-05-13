package PAC.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nume;
    
    @Column(unique = true)
    private String email;
    
    @Column(nullable = false)
    private String parola;
    
    @Column(nullable = false)
    private String rol;
    
    @Column(nullable = false)
    private String status;
}
