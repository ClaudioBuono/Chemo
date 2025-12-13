package connector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CmdExec {
    static final Logger logger = Logger.getLogger(CmdExec.class.getName());

    public static void main(final String[] args) {
        try{
            /*
            Viene prelevato il path del sistema che porta al modulo in python e viene mandato in esecuzione
            Per il path non è stata adottata una soluzione propriamente eleganete ma almeno funziona :)
             */
            final String path = Paths.get(System.getProperty("user.dir"), "py", "module.py").toString();
            final Process process = Runtime.getRuntime().exec(new String[]{"py", path});

            /*
            In questa sezione viene elaborato l'output prodotto dall'esecuzione del modulo
            */
            try(final BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                String line;
                final StringBuilder output = new StringBuilder(4096);

                while((line = input.readLine()) != null){
                    output.append(line).append("\n");
                }

                if (logger.isLoggable(Level.INFO)) {
                    logger.info(output.toString());
                }
            }
        }catch(final Exception err){
            logger.log(Level.SEVERE, err.getMessage(), err);
        }
    }

}
