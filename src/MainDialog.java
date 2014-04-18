import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Position;

class MainDialog
  extends JFrame
{
  private final JTextArea textArea = new JTextArea();
  
  public MainDialog()
    throws HeadlessException, IOException
  {
    super("Jenkins Console");
    
    JScrollPane pane = new JScrollPane(this.textArea);
    pane.setMinimumSize(new Dimension(400, 150));
    pane.setPreferredSize(new Dimension(400, 150));
    add(pane);
    
    setDefaultCloseOperation(3);
    setLocationByPlatform(true);
    
    PipedOutputStream out = new PipedOutputStream();
    PrintStream pout = new PrintStream(out);
    System.setErr(pout);
    System.setOut(pout);
    

    BufferedReader in = new BufferedReader(new InputStreamReader(new PipedInputStream(out)));
    new Thread()
    {
      private final BufferedReader val$in=null;
      
      public void run()
      {
        try
        {
          for (;;)
          {
            String line;
            if ((line = this.val$in.readLine()) != null)
            {
              String text = line;
              SwingUtilities.invokeLater(new Runnable()
              {
                private final String val$text=null;
                
                public void run()
                {
                  MainDialog.this.textArea.append(this.val$text + '\n');
                  MainDialog.this.scrollDown();
                }
              });
            }
          }
        }
        catch (IOException e)
        {
          throw new Error(e);
        }
      }
    }.start();
    pack();
  }
  
  private void scrollDown()
  {
    int pos = this.textArea.getDocument().getEndPosition().getOffset();
    this.textArea.getCaret().setDot(pos);
    this.textArea.requestFocus();
  }
}
