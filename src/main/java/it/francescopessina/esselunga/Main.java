package it.francescopessina.esselunga;

import com.sun.mail.smtp.SMTPTransport;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Main {

  private static WebDriver driver;

  // Secrets
  private static String ESSELUNGA_USERNAME = "<ESSELUNGA USERNAME>";
  private static String ESSELUNGA_PASSWORD = "<ESSELUNGA PASSWORD>";
  private static String MAIL_USERNAME = "<EMAIL ACCOUNT>";
  private static String MAIL_PASSWORD = "<EMAIL PASSWORD>";

  // Mail settings
  private static String SMTP_SERVER = "smtp.gmail.com";
  private static String EMAIL_FROM = "<SENDER EMAIL>";
  private static String EMAIL_TO = "<RECIPIENT EMAIL>";
  private static final String EMAIL_TO_CC = "";
  private static final String EMAIL_SUBJECT = "Slot Esselunga disponibili!!!";
  private static final String EMAIL_TEXT = "I seguenti slot sono disponibili! <br><br>";

  // Seconds to wait for an element to appear
  private static final int MAX_WAIT_TIME_SECONDS = 300;

  public static void main(String args[]) {
    readProperties();
    driver = new ChromeDriver();
    checkSlotAvailability();
    driver.close();
  }

  private static void readProperties() {

    try (InputStream input = Main.class.getClassLoader().getResourceAsStream("application.properties")) {
      Properties prop = new Properties();
      // load a properties file
      prop.load(input);
      // get the property value and print it out
      ESSELUNGA_USERNAME = prop.getProperty("esselunga.username");
      ESSELUNGA_PASSWORD = prop.getProperty("esselunga.password");
      SMTP_SERVER = prop.getProperty("mail.smtp");
      MAIL_USERNAME = prop.getProperty("mail.username");
      MAIL_PASSWORD = prop.getProperty("mail.password");
      EMAIL_FROM = prop.getProperty("mail.from");
      EMAIL_TO = prop.getProperty("mail.to");
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(0);
    }

  }

  public static void checkSlotAvailability() {

    driver.get("https://www.esselungaacasa.it/ecommerce/nav/welcome/index.html");
    driver.manage().window().setSize(new Dimension(1600, 860));
    driver.findElement(By.linkText("Accedi")).click();
    driver.findElement(By.id("gw_username")).click();

    driver.findElement(By.id("gw_username")).sendKeys(ESSELUNGA_USERNAME);
    driver.findElement(By.id("gw_password")).sendKeys(ESSELUNGA_PASSWORD);
    driver.findElement(By.cssSelector(".container-login-esterno > div:nth-child(2)")).click();
    driver.findElement(By.cssSelector("button")).click();

    WebDriverWait wait = new WebDriverWait(driver, MAX_WAIT_TIME_SECONDS);
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".icon-furgoncino")));

    driver.findElement(By.cssSelector(".icon-furgoncino")).click();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[id^=slot_")));

    List<WebElement> elements = driver.findElements(By.cssSelector("[id^=slot_]"));

    List<String> freeSlots = new ArrayList<>();

    for (WebElement e : elements) {
      WebElement radio = e.findElement(By.cssSelector("input"));
      String cssClass = radio.getAttribute("class");

      System.out.println("Element label: " + radio.getAttribute("aria-label") + ". Class: " + cssClass);

      if ((!cssClass.equals("esaurita")) && (!cssClass.equals("non-attiva"))) {
        freeSlots.add(radio.getAttribute("aria-label"));
      }
    }

    if (!freeSlots.isEmpty()) {
      System.out.println("Slots free!!1!1!!");
      sendMail(freeSlots);
    } else {
      System.out.println("No slots free.");
    }

  }

  private static void sendMail(List<String> freeSlots) {

    Properties prop = System.getProperties();
    prop.put("mail.smtp.host", SMTP_SERVER);
    prop.put("mail.smtp.port", "587");
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.starttls.enable", "true"); //TLS

    Session session = Session.getInstance(prop,
        new javax.mail.Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(MAIL_USERNAME, MAIL_PASSWORD);
          }
        });
    Message msg = new MimeMessage(session);

    try {
      msg.setFrom(new InternetAddress(EMAIL_FROM));
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO, false));
      msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(EMAIL_TO_CC, false));
      msg.setSubject(EMAIL_SUBJECT);
      String text = EMAIL_TEXT;
      for (String slot : freeSlots) {
        text += slot + "<br>";
      }
      msg.setDataHandler(new DataHandler(new HTMLDataSource(text)));
      msg.setSentDate(new Date());

      SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
      t.connect(SMTP_SERVER, MAIL_USERNAME, MAIL_PASSWORD);
      t.sendMessage(msg, msg.getAllRecipients());
      System.out.println("Mail sent to " + EMAIL_TO + ".Response: " + t.getLastServerResponse());
      t.close();
    } catch (MessagingException e) {
      e.printStackTrace();
    }

  }

  static class HTMLDataSource implements DataSource {

    private String html;

    public HTMLDataSource(String htmlString) {
      html = htmlString;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      if (html == null) throw new IOException("html message is null!");
      return new ByteArrayInputStream(html.getBytes());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new IOException("This DataHandler cannot write HTML");
    }

    @Override
    public String getContentType() {
      return "text/html";
    }

    @Override
    public String getName() {
      return "HTMLDataSource";
    }
  }

}
