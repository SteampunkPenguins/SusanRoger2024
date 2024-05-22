
package frc.robot;

import com.revrobotics.CANSparkLowLevel.MotorType;
// Imports for motors
import com.revrobotics.CANSparkMax;
//import com.revrobotics.CANSparkMax.Base;
// import for relative encoder
import com.revrobotics.RelativeEncoder;
//test for slewratelimiter
import edu.wpi.first.math.filter.SlewRateLimiter ;
import edu.wpi.first.net.PortForwarder;
// imports for limelight
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.HttpCamera;
import edu.wpi.first.cscore.VideoSource;
// import for blinkin
import edu.wpi.first.wpilibj.PWM;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
// Imports for NavX
//import com.kauailabs.navx.frc.AHRS;
// Shuffleboard Variables 
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;



public class Robot extends TimedRobot { 

  //private final String kDefaultAuto;
  private final CANSparkMax frontLeft = new CANSparkMax(6, MotorType.kBrushless); //CAN ID is set on the sparkmax 
  private final CANSparkMax backLeft = new CANSparkMax(5, MotorType.kBrushless); // Motortype must also be set for the neo it is brushless.
  private final CANSparkMax frontRight = new CANSparkMax(8, MotorType.kBrushless);
  private final CANSparkMax backRight = new CANSparkMax(7, MotorType.kBrushless);
  private final CANSparkMax shooterFront = new CANSparkMax(4, MotorType.kBrushless);
  private final CANSparkMax shooterBack = new CANSparkMax(3, MotorType.kBrushless);
  private final CANSparkMax climber = new CANSparkMax(2, MotorType.kBrushless);
  private final CANSparkMax amp = new CANSparkMax(9, MotorType.kBrushed);

  // Encoder for climber
  private final RelativeEncoder climberEncoder = climber.getEncoder();
  private final RelativeEncoder backEncoder = shooterBack.getEncoder();
  private final RelativeEncoder fronEncoder = shooterFront.getEncoder();

  //current limits for motors

  //how many amps can an individual drivetrain motor uses
  static final int DRIVE_CURRENT_LIMIT_A = 60;
  //how many amps the feeder motor can use
  static final int FEEDER_CURRENT_LIMIT_A = 80;
  //percent output to run the feeder when expelling note
  static final double FEEDER_OUT_SPEED = 1.0;
  //percent output to run the feeder when intaking note
  static final double FEEDER_IN_SPEED = .4;
  //how many amps the launcher motor can use
  static final int LAUNCHER_CURRENT_LIMIT_A = 80;
  //percent output torun the launcher when intaking and expelling note
  static final double LAUNCHER_SPEED = 1.0;
  static final double LAUNCHER_AMP_SPEED = .17;
  //percent output to power the climber
  static final double CLIMBER_OUTPUT_POWER = 1;

  // In our code for last year we used motor control groups. That has been discontinued. We now must use 'follow' to link the two motors.
  private final DifferentialDrive yangMobile = new DifferentialDrive(frontLeft,frontRight); // Differential drive is the main object to control the drivetrain.
  // For now we have one controller created for a driver.
  private final XboxController driver = new XboxController(0);
  private final XboxController player = new XboxController(1);
  //private final String m_autoSelected;
  private final Timer m_timer = new Timer();
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx");
  NetworkTableEntry ty = table.getEntry("ty");
  NetworkTableEntry ta = table.getEntry("ta");
  //swicth inputs
  private final DigitalInput bellernote = new DigitalInput(0);
  //private final CameraServer camera = new CameraServer();
  
  //private final double color = 0.0;
  private final PWM colorController = new PWM(9);
  private final double glitter = -0.89; // red = -0.85
  private final double hotPink = 0.57;

  //automous selection options

  private static final String autoShoot = "autoShoot";
  private static final String autoBackup = "autoBackup";
  private static final String doNothing = "doNothing";
  private static final String shootOnly = "shootOnly";
  private String bellerika = "yuh";
  private final SendableChooser<String> m_Chooser = new SendableChooser<>();

  //encoder??
  double diameter = 6/12; //6 inch wheels
  double dist = 0.5*3.14/1024; //feet per pulse
  
  // test to smooth out drivetrain
  private final SlewRateLimiter filterY = new SlewRateLimiter(0.80);
  // Two slew filters caused control problems.
  //private final SlewRateLimiter filterX = new SlewRateLimiter(0.50);

  //Camera Server
  HttpCamera httpCamera;
  
  @Override
  public void robotInit() {
    // In order to use four motors for the drivetrain each side must follow the motor which is contolled by arcadeDrive. 
    backRight.follow(frontRight); //Back motors follow; front motors are controlled.
    backLeft.follow(frontLeft);
    
    httpCamera = new HttpCamera("Limelight :DDD", "http://10.32.4.208:5800");

    //CameraServer.startAutomaticCapture(0);
    Shuffleboard.getTab("Tab").add(httpCamera);

    // For differential drive to work the right side must be inverted
    frontRight.setInverted(true);
    backRight.setInverted(true); // note Mr.O is not sure if the back motor needs to be inverted. Experiment if there is time.
    shooterFront.setInverted(false);

    // Set current limits for the motors
    frontRight.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    backRight.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    frontLeft.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    backRight.setSmartCurrentLimit(DRIVE_CURRENT_LIMIT_A);
    shooterBack.setSmartCurrentLimit(FEEDER_CURRENT_LIMIT_A);
    shooterFront.setSmartCurrentLimit(LAUNCHER_CURRENT_LIMIT_A);
    amp.setSmartCurrentLimit(LAUNCHER_CURRENT_LIMIT_A);
    //climber.setSmartCurrentLimit(CLIMBER_OUTPUT_POWER);




    // Make sure you only configure port forwarding once in your robot code. Do not place these function calls in any periodic functions
    // Port configuration should coorespond to the port configured on the limelight
    for (int port = 5800; port <= 5807; port++) {
      PortForwarder.add(port, "limelight.local", port);
      }

//AUTO CODE
    m_Chooser.setDefaultOption("Shoot", autoShoot);

    m_Chooser.addOption("Shoot", autoShoot);
    m_Chooser.addOption("Back Up", autoBackup);
    m_Chooser.addOption("Do Nothing", doNothing);
    m_Chooser.addOption("Only Shoot", shootOnly);
    SmartDashboard.putData("Auto choices", m_Chooser);
    
  }
  @Override
  public void robotPeriodic() {
    //read values periodically
    //double target = tv.getDouble(0.0); // values 0 or 1. evaluates 1 if there is a valid target
    double x = tx.getDouble(0.0); //relative x coorinates of the april from the perspecitve of the roboot
    double y = ty.getDouble(0.0); //relative x coorinates of the april from the perspecitve of the roboot
    double area = ta.getDouble(0.0); // area calculation for the field of view of the lime light

    //post to smart dashboard periodically
    SmartDashboard.putNumber("LimelightX", x);
    SmartDashboard.putNumber("LimelightY", y);
    SmartDashboard.putNumber("LimelightArea", area);
    SmartDashboard.putBoolean("Note?", bellernote.get());
    SmartDashboard.putString("bellerika?", bellerika);
    SmartDashboard.putNumber("Climber Position", climberEncoder.getPosition());
    SmartDashboard.putNumber("Back Shooter", backEncoder.getPosition());
    SmartDashboard.putNumber("front Shooter", fronEncoder.getPosition());
    //CameraServer.getInstance().addCamera(camera);
    // SmartDashboard.getTab("Tab").add(httpCamera);
    //ShuffleboardTab.putSource(httpCamera);
    //
    if (bellernote.get()) {
      
      colorController.setSpeed(glitter);
    }
    else {
      colorController.setSpeed(hotPink);
    }

    
  }

  @Override
  public void autonomousInit() {
    m_timer.reset();
    m_timer.start();
    //go backwards
    frontLeft.setInverted(true);
    frontRight.setInverted(false);
    
    bellerika = m_Chooser.getSelected();


    //m_autoSelected = m_chooser.getSelected();
   // for smart dashboard 
    //m_autoSelected = SmartDashboard.getString("Auto Seletor ", kDefaultAuto ) ;
    //System.out.println( "Auto selected:" + m_autoSelected) ; 

  }
  @Override
  public void autonomousPeriodic() {
    if (bellerika == autoShoot) {
      if (( m_timer.get() > 0) && (m_timer.get() < 2)) { 
        //(not fully charged) 1 second = 6.5 ft
        //full battery: 1 second = 7 ft.
        shooterFront.set(1.0); 
      }
      if ((m_timer.get() > 2) && (m_timer.get() < 3)){
        shooterBack.set(0.8);
      }
      if (( m_timer.get() > 3) && (m_timer.get() < 10)) { 
        shooterFront.stopMotor();
        shooterBack.stopMotor();
        //yangMobile.arcadeDrive (0.5, 0.0); commented out for demo
        }
        //else {

        //}
      
    //} else if (bellerika == autoBackup) { //bellerika is false
      //if (( m_timer.get() > 1) && (m_timer.get() < 2)) { 
        //yangMobile.arcadeDrive (0.5, 0.0); commented out for demo
    //}
    //else if (bellerika == shootOnly) { //bellerika is false
      //if (( m_timer.get() > 1) && (m_timer.get() < 2)) {
      //}
    //}
    }
    }
  
  @Override
  public void teleopInit() {
    climberEncoder.setPosition(0);
  }
  @Override
  public void teleopPeriodic() { 
    //test for slew rate limiter
    //The drivetrain is controlled by arcadedrive in teleopPeriodic. 
    //yangMobile.arcadeDrive(filterY.calculate(-driver.getLeftY()),-driver.getRightX()/2); commented out for demo
    // As it is now the left joystick up and down (y axis) controls acceleration.
    // The right joystick left and right (x axis) controls turning.
    //april tags 7 and 8 are blue speaker
    //april tags 3 and 4 are red speaker
    if(player.getBButtonPressed()) {
      //shooterFront.set(1.0);
      shooterBack.set(FEEDER_OUT_SPEED);
    }
    else if (player.getBButtonReleased()){
      //shooterFront.stopMotor();
      shooterBack.stopMotor();
    }
    if(player.getLeftBumper()) {
      shooterFront.set(LAUNCHER_SPEED);
      //shooterBack.set(0.5);
    }
    else if (player.getLeftBumperReleased()){
      shooterFront.stopMotor();
      //shooterBack.stopMotor();
    }
    if(player.getXButton()){
      shooterFront.setInverted(true);
      shooterFront.set(FEEDER_IN_SPEED);
      shooterBack.setInverted(true);
      shooterBack.set(FEEDER_IN_SPEED);
    }
    else if (player.getXButtonReleased()){
      shooterFront.stopMotor();
      shooterFront.setInverted(false);
      shooterBack.stopMotor();
      shooterBack.setInverted(false);
  
    }

  //april tag 5 is red amp
  //april tag 6 is blue amp
  /*if (player.getYButton()){
    amp.set(0.5);
  }
  else if (player.getYButtonReleased()){
    amp.stopMotor();
  }
  if (player.getAButton()){
    amp.setInverted(true);
    amp.set(0.5);
  }
  else if (player.getAButtonReleased()){
    amp.setInverted(false);
     n 0.stopMotor();
  }  
  */
  // change to same use joystick code and conditional in order to invert the motor.
  if (driver.getRightBumperPressed()){
    climber.setInverted(true);
    climber.set(1);
  }
  else if (driver.getRightBumperReleased()){
    climber.stopMotor();
  }
  if (driver.getYButtonPressed()) {
    climberEncoder.setPosition(0);
  }

  if (driver.getLeftBumperPressed()) {
    climber.setInverted(false);
    climber.set(1);
  }
  else if (driver.getLeftBumperReleased()){
    climber.stopMotor();
  }

}

  }
