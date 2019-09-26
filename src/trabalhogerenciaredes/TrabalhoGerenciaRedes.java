package trabalhogerenciaredes;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;

// Otávio da Cruz Mello
// Gerência de Redes - Trabalho I
// Desenvolvido em Sistema Operacional Windows
public class TrabalhoGerenciaRedes {

    public static void main(String args[]) throws UnknownHostException {
        Scanner periodicidade = new Scanner(System.in);

        // Define de quanto em quanto tempo a varredura de dispositivos conectados será realizada
        System.out.println("Defina a periodicidade da varredura (em segs)");
        while (!periodicidade.hasNextInt()) {
            periodicidade.next();
        }
        int tempo = periodicidade.nextInt();

        File f1 = new File("src/trabalhogerenciaredes/historyDevices.txt");
        if (f1.exists()) {
            f1.delete();
        }
        // Chama a função a cada X segundos definidos pelo usuário, é importante lembrar que mesmo que o tempo
        // tenha sido definido pelo usuário, a próxima varredura só será realizada quando a anterior finalizar
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new searchNetwork(), 0, tempo * 1000);
    }
}
