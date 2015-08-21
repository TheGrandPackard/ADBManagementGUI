import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;

public class AndroidShell  {
   private ProcessBuilder builder;
   private Process adb;
   private String ip;
   private static final byte[] LS = "\n".getBytes();

   private OutputStream processInput;
   private InputStream processOutput;

   private Thread t;
   
   public AndroidShell(String ip)
   {
	   this.ip = ip;
   }

   public void start() throws IOException  {
      builder = new ProcessBuilder("adb", "-s", (this.ip + ":5555"), "shell");
      adb = builder.start();

      // reads from the process output
      processInput = adb.getOutputStream();

      // sends to process's input
      processOutput = adb.getInputStream();

      // thread that reads process's output and prints it to system.out
      Thread t = new Thread() {
         public void run() {
            try   {
               int c = 0;
               byte[] buffer = new byte[2048];
               while((c = processOutput.read(buffer)) != -1) {
                     System.out.write(buffer, 0, c);
               }
            }catch(Exception e)  { e.printStackTrace();}
         }
      };
      t.start();
   }
   
   public void stop()   {
      try   {
         if(processOutput != null && t != null) {
            this.execCommand("exit");
            processOutput.close();
         }
      }catch(Exception ignore)  {}
   }
   
   public void execCommand(String adbCommand) throws IOException {
      processInput.write(adbCommand.getBytes());
      processInput.write(LS);
      processInput.flush();
   }

   public static void disconnectDevices() {
	   ProcessBuilder builder = new ProcessBuilder("adb", "disconnect");
		builder.redirectOutput(Redirect.INHERIT);
		
		try {
			builder.start();
			System.out.println("Disconnected devices");
		} catch (IOException e) {
			e.printStackTrace();
		}
   }

   public void connectToDevice(String ip) {
	   
	   //disconnectDevices();
	   
	   ProcessBuilder builder = new ProcessBuilder("adb", "connect", ip);
	   builder.redirectOutput(Redirect.INHERIT);
		
	   try {
		   builder.start();	
		   Thread.sleep(1000);
	   } catch (IOException | InterruptedException e) {
		   e.printStackTrace();
	   }
   }
   
   public void executeCommands(String[] adbCommands) throws IOException, InterruptedException {
	   
	   for(String command : adbCommands)  {
	         if(command.startsWith("sleep"))   {
	            String sleep = command.split(" ")[1].trim();
	            long sleepTime = Integer.parseInt(sleep) * 1000;
	            Thread.sleep(sleepTime);
	         }else {
	            this.execCommand(command);
	         }
	      }
   }
   
   public static void main(String[] args) throws Exception  {
	   	   
	  String[] write = {	"su", 
			  				"( netcfg|grep wlan0; uptime; )",
			  				//cat /data/misc/wifi/wpa_supplicant.conf
			  				//"sed -i 's/update_config=1/update_config=1\\nap_scan=2/' /data/misc/wifi/wpa_supplicant.conf", 
			  				//"cat /data/misc/wifi/wpa_supplicant.conf", 
			  				"reboot"
			  				};
	  
	  String[] read = {	"su", 
						"( netcfg|grep wlan0; uptime; )",
						//cat /data/misc/wifi/wpa_supplicant.conf
						"exit",
						"exit" 
						};
	  	  
	  String[] devices = {	"IP_ADDRESSES_GOES_HERE" };
	  
	  for(String ip : devices) {
		  AndroidShell shell = new AndroidShell(ip);
	      shell.connectToDevice(ip);
	      shell.start();
	      shell.executeCommands(write);
	      //shell.stop();
	  }
   }
}