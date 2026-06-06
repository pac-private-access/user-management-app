package PAC.model;

import java.sql.Time;
import java.time.DayOfWeek;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "employees")
public class Employee{
    
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String nume;
	
    @Column(nullable = false)
    private String cnp;
    
    @Column(nullable = false)
    private String badgeNumber;
    
    @Column(nullable = false)
    private String bluetoothSecurityCode;
    
    @Column(nullable = false)
    private int divisionId;
    
    @Column(nullable = false)
    private boolean isAccessActive;
    
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;
    
    @Column(nullable = false)
    private Time scheduleStartTime;
    
    @Column(nullable = false)
    private Time scheduleEndTime;
    
    @Column(nullable = false)
    private EmployeeRole role;
}
