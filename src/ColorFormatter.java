import java.lang.reflect.Method;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ColorFormatter
  extends SimpleFormatter
{
  public String format(LogRecord record)
  {
    String body = super.format(record);
    int v = record.getLevel().intValue();
    if (v >= SEVERE) {
      return "\033[31m" + body + "\033[0m";
    }
    if (v >= WARNING) {
      return "\033[33m" + body + "\033[0m";
    }
    return body;
  }
  
  public static void install()
  {
    try
    {
      if (System.class.getMethod("console", new Class[0]).invoke(null, new Object[0]) == null) {
        return;
      }
    }
    catch (Throwable t)
    {
      return;
    }
    Handler[] handlers = Logger.getLogger("").getHandlers();
    for (int i = 0; i < handlers.length; i++)
    {
      Handler h = handlers[i];
      if ((h.getClass() == ConsoleHandler.class) && 
        (h.getFormatter().getClass() == SimpleFormatter.class)) {
        h.setFormatter(new ColorFormatter());
      }
    }
  }
  
  private static final int SEVERE = Level.SEVERE.intValue();
  private static final int WARNING = Level.WARNING.intValue();
}
