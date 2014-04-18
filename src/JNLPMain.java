import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class JNLPMain
{
  private static final String PROMINENT_WINDOWS_INSTALLER = "hudson.lifecycle.WindowsInstallerLink.prominent";
  
  public static void main(String[] args)
    throws Exception
  {
    try
    {
      System.setSecurityManager(null);
    }
    catch (SecurityException e) {}
    if (System.getProperty("hudson.lifecycle.WindowsInstallerLink.prominent") == null) {
      System.setProperty("hudson.lifecycle.WindowsInstallerLink.prominent", "true");
    }
    boolean headlessMode = Boolean.getBoolean("hudson.webstart.headless");
    if (!headlessMode)
    {
      setUILookAndFeel();
      new MainDialog().setVisible(true);
    }
    Main.main(args);
  }
  
  public static void setUILookAndFeel()
  {
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (InstantiationException e) {}catch (ClassNotFoundException e) {}catch (UnsupportedLookAndFeelException e) {}catch (IllegalAccessException e) {}
  }
}
