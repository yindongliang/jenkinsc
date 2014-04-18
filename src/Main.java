import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Main
{
  private static boolean hasLogOption(String[] args)
  {
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("--logfile=")) {
        return true;
      }
    }
    return false;
  }
  
  private static Map parseDependencyVersions()
    throws IOException
  {
    Map r = new HashMap();
    BufferedReader in = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("WEB-INF/classes/dependencies.txt")));
    String line;
    while ((line = in.readLine()) != null)
    {
      line = line.trim();
      String[] tokens = line.split(":");
      if (tokens.length == 5) {
        r.put(tokens[0] + ":" + tokens[1], tokens[3]);
      }
    }
    return r;
  }
  
  public static void main(String[] args)
    throws Exception
  {
    try
    {
      String v = System.getProperty("java.class.version");
      if (v != null) {
        try
        {
          if (Float.parseFloat(v) < 49.0F) {
            throw new UnsupportedClassVersionError(v);
          }
        }
        catch (NumberFormatException e) {}
      }
      ColorFormatter.install();
      
      _main(args);
    }
    catch (UnsupportedClassVersionError e)
    {
      System.err.println("Jenkins requires Java5 or later, but you are running " + System.getProperty("java.runtime.version") + " from " + System.getProperty("java.home"));
      
      e.printStackTrace();
    }
  }
  
  private static void _main(String[] args)
    throws Exception
  {
    File extractedFilesFolder = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("--extractedFilesFolder="))
      {
        extractedFilesFolder = new File(args[i].substring("--extractedFilesFolder=".length()));
        if (!extractedFilesFolder.isDirectory())
        {
          System.err.println("The extractedFilesFolder value is not a directory. Ignoring.");
          extractedFilesFolder = null;
        }
      }
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("--daemon"))
      {
        Map revisions = parseDependencyVersions();
        

        ClassLoader cl = new URLClassLoader(new URL[] { extractFromJar("WEB-INF/lib/jna-" + getVersion(revisions, "net.java.dev.jna", "jna") + ".jar", "jna", "jar", extractedFilesFolder).toURI().toURL(), extractFromJar("WEB-INF/lib/akuma-" + getVersion(revisions, "org.kohsuke", "akuma") + ".jar", "akuma", "jar", extractedFilesFolder).toURI().toURL() });
        


        Class $daemon = cl.loadClass("com.sun.akuma.Daemon");
        Object daemon = $daemon.newInstance();
        

        Method isDaemonized = $daemon.getMethod("isDaemonized", new Class[0]);
        if (!((Boolean)isDaemonized.invoke(daemon, new Object[0])).booleanValue())
        {
          System.out.println("Forking into background to run as a daemon.");
          if (!hasLogOption(args)) {
            System.out.println("Use --logfile to redirect output to a file");
          }
        }
        Method m = $daemon.getMethod("all", new Class[] { Boolean.TYPE });
        m.invoke(daemon, new Object[] { Boolean.TRUE });
      }
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("--logfile="))
      {
        LogFileOutputStream los = new LogFileOutputStream(new File(args[i].substring("--logfile=".length())));
        PrintStream ps = new PrintStream(los);
        System.setOut(ps);
        System.setErr(ps);
        
        List _args = new ArrayList(Arrays.asList(args));
        _args.remove(i);
        args = (String[])_args.toArray(new String[_args.size()]);
        break;
      }
    }
    System.setProperty("java.awt.headless", "true");
    if (System.getProperty("hudson.diyChunking") == null) {
      System.setProperty("hudson.diyChunking", "true");
    }
    File me = whoAmI(extractedFilesFolder);
    System.out.println("Running from: " + me);
    System.setProperty("executable-war", me.getAbsolutePath());
    

    List arguments = new ArrayList(Arrays.asList(args));
    trimOffOurOptions(arguments);
    arguments.add(0, "--warfile=" + me.getAbsolutePath());
    if (!hasWebRoot(arguments))
    {
      Main.FileAndDescription describedHomeDir = getHomeDir();
      System.out.println("webroot: " + describedHomeDir.description);
      arguments.add("--webroot=" + new File(describedHomeDir.file, "war"));
    }
    if (arguments.contains("--version"))
    {
      System.out.println(getVersion("?"));
      return;
    }
    File tmpJar = extractFromJar("winstone.jar", "winstone", ".jar", extractedFilesFolder);
    



    File tempFile = File.createTempFile("dummy", "dummy");
    deleteContents(new File(tempFile.getParent(), "winstone/" + me.getName()));
    tempFile.delete();
    

    ClassLoader cl = new URLClassLoader(new URL[] { tmpJar.toURI().toURL() });
    Class launcher = cl.loadClass("winstone.Launcher");
    Method mainMethod = launcher.getMethod("main", new Class[] { new String[0].getClass() });
    

    Field usage = launcher.getField("USAGE");
    usage.set(null, "Jenkins Continuous Integration Engine " + getVersion("") + "\n" + "Usage: java -jar jenkins.war [--option=value] [--option=value]\n" + "\n" + "Options:\n" + "   --extractedFilesFolder   = folder where extracted files are to be located. Default is the temp folder\n" + "   --daemon                 = fork into background and run as daemon (Unix only)\n" + "   --config                 = load configuration properties from here. Default is ./winstone.properties\n" + "   --prefix                 = add this prefix to all URLs (eg http://localhost:8080/prefix/resource). Default is none\n" + "   --commonLibFolder        = folder for additional jar files. Default is ./lib\n" + "   \n" + "   --logfile                = redirect log messages to this file\n" + "   --logThrowingLineNo      = show the line no that logged the message (slow). Default is false\n" + "   --logThrowingThread      = show the thread that logged the message. Default is false\n" + "   --debug                  = set the level of debug msgs (1-9). Default is 5 (INFO level)\n" + "\n" + "   --httpPort               = set the http listening port. -1 to disable, Default is 8080\n" + "   --httpListenAddress      = set the http listening address. Default is all interfaces\n" + "   --httpDoHostnameLookups  = enable host name lookups on incoming http connections (true/false). Default is false\n" + "   --httpKeepAliveTimeout   = how long idle HTTP keep-alive connections are kept around (in ms; default 5000)?\n" + "   --httpsPort              = set the https listening port. -1 to disable, Default is disabled\n" + "                              if neither --httpsCertificate nor --httpsKeyStore are specified,\n" + "                              https is run with one-time self-signed certificate.\n" + "   --httpsListenAddress     = set the https listening address. Default is all interfaces\n" + "   --httpsDoHostnameLookups = enable host name lookups on incoming https connections (true/false). Default is false\n" + "   --httpsKeepAliveTimeout   = how long idle HTTPS keep-alive connections are kept around (in ms; default 5000)?\n" + "   --httpsKeyStore          = the location of the SSL KeyStore file.\n" + "   --httpsKeyStorePassword  = the password for the SSL KeyStore file. Default is null\n" + "   --httpsCertificate       = the location of the PEM-encoded SSL certificate file.\n" + "                              (the one that starts with '-----BEGIN CERTIFICATE-----')\n" + "                              must be used with --httpsPrivateKey.\n" + "   --httpsPrivateKey        = the location of the PEM-encoded SSL private key.\n" + "                              (the one that starts with '-----BEGIN RSA PRIVATE KEY-----')\n" + "   --httpsKeyManagerType    = the SSL KeyManagerFactory type (eg SunX509, IbmX509). Default is SunX509\n" + "   --spdy                   = Enable SPDY. See http://wiki.eclipse.org/Jetty/Feature/NPN\n" + "   --ajp13Port              = set the ajp13 listening port. -1 to disable, Default is disabled\n" + "   --ajp13ListenAddress     = set the ajp13 listening address. Default is all interfaces\n" + "   --controlPort            = set the shutdown/control port. -1 to disable, Default disabled\n" + "   \n" + "   --handlerCountStartup    = set the no of worker threads to spawn at startup. Default is 5\n" + "   --handlerCountMax        = set the max no of worker threads to allow. Default is 40\n" + "   --handlerCountMaxIdle    = set the max no of idle worker threads to allow. Default is 5\n" + "   \n" + "   --sessionTimeout         = set the http session timeout value in minutes. Default to what webapp specifies, and then to 60 minutes\n" + "   --mimeTypes=ARG          = define additional MIME type mappings. ARG would be EXT=MIMETYPE:EXT=MIMETYPE:...\n" + "                              (e.g., xls=application/vnd.ms-excel:wmf=application/x-msmetafile)\n" + "   --maxParamCount=N        = set the max number of parameters allowed in a form submission to protect\n" + "                              against hash DoS attack (oCERT #2011-003). Default is 10000.\n" + "   --usage / --help         = show this message\n" + "   --version                = show the version and quit\n" + "   \n" + "Security options:\n" + "   --realmClassName               = Set the realm class to use for user authentication. Defaults to ArgumentsRealm class\n" + "   \n" + "   --argumentsRealm.passwd.<user> = Password for user <user>. Only valid for the ArgumentsRealm realm class\n" + "   --argumentsRealm.roles.<user>  = Roles for user <user> (comma separated). Only valid for the ArgumentsRealm realm class\n" + "   \n" + "   --fileRealm.configFile         = File containing users/passwds/roles. Only valid for the FileRealm realm class\n" + "   \n" + "Access logging:\n" + "   --accessLoggerClassName        = Set the access logger class to use for user authentication. Defaults to disabled\n" + "   --simpleAccessLogger.format    = The log format to use. Supports combined/common/resin/custom (SimpleAccessLogger only)\n" + "   --simpleAccessLogger.file      = The location pattern for the log file(SimpleAccessLogger only)");
    try
    {
      Field f = cl.loadClass("winstone.WinstoneSession").getField("SESSION_COOKIE_NAME");
      f.setAccessible(true);
      f.set(null, "JSESSIONID." + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
    }
    catch (ClassNotFoundException e) {}
    mainMethod.invoke(null, new Object[] { arguments.toArray(new String[0]) });
  }
  
  private static void trimOffOurOptions(List arguments)
  {
    for (Iterator itr = arguments.iterator(); itr.hasNext();)
    {
      String arg = (String)itr.next();
      if ((arg.startsWith("--daemon")) || (arg.startsWith("--logfile")) || (arg.startsWith("--extractedFilesFolder"))) {
        itr.remove();
      }
    }
  }
  
  private static String getVersion(Map revisions, String groupId, String artifactId)
  {
    String v = (String)revisions.get(groupId + ":" + artifactId);
    Iterator itr;
    if (v == null) {
      for (itr = revisions.keySet().iterator(); itr.hasNext();)
      {
        String key = (String)itr.next();
        if (key.endsWith(":" + artifactId)) {
          return v;
        }
      }
    }
    return v;
  }
  
  private static String getVersion(String fallback)
    throws IOException
  {
    Enumeration manifests = Main.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
    while (manifests.hasMoreElements())
    {
      URL res = (URL)manifests.nextElement();
      Manifest manifest = new Manifest(res.openStream());
      String v = manifest.getMainAttributes().getValue("Jenkins-Version");
      if (v != null) {
        return v;
      }
    }
    return fallback;
  }
  
  private static boolean hasWebRoot(List arguments)
  {
    for (Iterator itr = arguments.iterator(); itr.hasNext();)
    {
      String s = (String)itr.next();
      if (s.startsWith("--webroot=")) {
        return true;
      }
    }
    return false;
  }
  
  public static File whoAmI(File directory)
    throws IOException, URISyntaxException
  {
    try
    {
      URL classFile = Main.class.getClassLoader().getResource("Main.class");
      JarFile jf = ((JarURLConnection)classFile.openConnection()).getJarFile();
      Field f = ZipFile.class.getDeclaredField("name");
      f.setAccessible(true);
      return new File((String)f.get(jf));
    }
    catch (Exception x)
    {
      System.err.println("ZipFile.name trick did not work, using fallback: " + x);
      
      File myself = File.createTempFile("jenkins", ".jar", directory);
      myself.deleteOnExit();
      InputStream is = Main.class.getProtectionDomain().getCodeSource().getLocation().openStream();
      try
      {
        OutputStream os = new FileOutputStream(myself);
        try
        {
          copyStream(is, os);
        }
        finally {}
      }
      finally
      {
        is.close();
      }
      return myself;
    }
  }
  
  private static void copyStream(InputStream in, OutputStream out)
    throws IOException
  {
    byte[] buf = new byte[8192];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
  }
  
  private static File extractFromJar(String resource, String fileName, String suffix, File directory)
    throws IOException
  {
    URL res = Main.class.getResource(resource);
    if (res == null) {
      throw new IOException("Unable to find the resource: " + resource);
    }
    File tmp;
    try
    {
      tmp = File.createTempFile(fileName, suffix, directory);
    }
    catch (IOException e)
    {
      String tmpdir = directory == null ? System.getProperty("java.io.tmpdir") : directory.getAbsolutePath();
      IOException x = new IOException("Jenkins has failed to create a temporary file in " + tmpdir);
      x.initCause(e);
      throw x;
    }
    InputStream is = res.openStream();
    try
    {
      OutputStream os = new FileOutputStream(tmp);
      try
      {
        copyStream(is, os);
      }
      finally {}
    }
    finally
    {
      is.close();
    }
    tmp.deleteOnExit();
    return tmp;
  }
  
  private static void deleteContents(File file)
    throws IOException
  {
    if (file.isDirectory())
    {
      File[] files = file.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          deleteContents(files[i]);
        }
      }
    }
    file.delete();
  }
  
  private static class FileAndDescription
  {
    File file;
    String description;
    
    public FileAndDescription(File file, String description)
    {
      this.file = file;
      this.description = description;
    }
  }
  
  private static Main.FileAndDescription getHomeDir()
  {
    for (int i = 0; i < HOME_NAMES.length; i++)
    {
      String name = HOME_NAMES[i];
      try
      {
        InitialContext iniCtxt = new InitialContext();
        Context env = (Context)iniCtxt.lookup("java:comp/env");
        String value = (String)env.lookup(name);
        if ((value != null) && (value.trim().length() > 0)) {
          return new Main.FileAndDescription(new File(value.trim()), "JNDI/java:comp/env/" + name);
        }
        value = (String)iniCtxt.lookup(name);
        if ((value != null) && (value.trim().length() > 0)) {
          return new Main.FileAndDescription(new File(value.trim()), "JNDI/" + name);
        }
      }
      catch (NamingException e) {}
    }
    for (int i = 0; i < HOME_NAMES.length; i++)
    {
      String name = HOME_NAMES[i];
      String sysProp = System.getProperty(name);
      if (sysProp != null) {
        return new Main.FileAndDescription(new File(sysProp.trim()), "System.getProperty(\"" + name + "\")");
      }
    }
    try
    {
      for (int i = 0; i < HOME_NAMES.length; i++)
      {
        String name = HOME_NAMES[i];
        String env = System.getenv(name);
        if (env != null) {
          return new Main.FileAndDescription(new File(env.trim()).getAbsoluteFile(), "EnvVars.masterEnvVars.get(\"" + name + "\")");
        }
      }
    }
    catch (Throwable e) {}
    File legacyHome = new File(new File(System.getProperty("user.home")), ".hudson");
    if (legacyHome.exists()) {
      return new Main.FileAndDescription(legacyHome, "$user.home/.hudson");
    }
    File newHome = new File(new File(System.getProperty("user.home")), ".jenkins");
    return new Main.FileAndDescription(newHome, "$user.home/.jenkins");
  }
  
  private static final String[] HOME_NAMES = { "JENKINS_HOME", "HUDSON_HOME" };
}
