import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import sun.misc.Signal;
import sun.misc.SignalHandler;

final class LogFileOutputStream
  extends FilterOutputStream
{
  private final File file;
  
  LogFileOutputStream(File file)
    throws FileNotFoundException
  {
    super(null);
    this.file = file;
    this.out = new FileOutputStream(file, true);
    if (File.pathSeparatorChar == ':') {
      Signal.handle(new Signal("ALRM"), new SignalHandler()
      {
        public void handle(Signal signal)
        {
          try
          {
            LogFileOutputStream.this.reopen();
          }
          catch (IOException e)
          {
            throw new Error(e);
          }
        }
      });
    }
  }
  
  public synchronized void reopen()
    throws IOException
  {
    this.out.close();
    this.out = NULL;
    this.out = new FileOutputStream(this.file, true);
  }
  
  public synchronized void write(byte[] b)
    throws IOException
  {
    this.out.write(b);
  }
  
  public synchronized void write(byte[] b, int off, int len)
    throws IOException
  {
    this.out.write(b, off, len);
  }
  
  public synchronized void flush()
    throws IOException
  {
    this.out.flush();
  }
  
  public synchronized void close()
    throws IOException
  {
    this.out.close();
  }
  
  public synchronized void write(int b)
    throws IOException
  {
    this.out.write(b);
  }
  
  public String toString()
  {
    return getClass().getName() + " -> " + this.file;
  }
  
  private static final OutputStream NULL = new OutputStream()
  {
    public void write(int b)
      throws IOException
    {}
    
    public void write(byte[] b, int off, int len)
      throws IOException
    {}
  };
}
