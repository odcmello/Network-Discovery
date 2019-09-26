/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhogerenciaredes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Otavio
 */
public class searchNetwork extends TimerTask {

    // Tabela ARP encontrada em uma determinada execução do programa, esta será comparada com tabela global, que mostrará todo o histórico
    ArrayList<Dispositivo> dispositivosConectados;

    @Override
    public void run() {
        dispositivosConectados = new ArrayList();
        String ip = new String();
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            Logger.getLogger(searchNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Verifica se a máquina está conectada a rede
        if (ip.equals("127.0.0.1")) {
            System.out.println("O computador não está conectado a rede.");
        } else {
            System.out.println("Iniciando nova varredura...");
            String ipinicial = getInitialIP(ip); // Constrói a parte inicial do endereço IP a partir do IP do dispositivo do usuário
            pingIPs(ipinicial); // Faz ping de todos os IPs da rede para atualizar na tabela ARP e ver quais novos/velhos
            arpTable(); // Exibe a tabela ARP com todos os dispositivos conectados a rede, seus endereços IP e endereços MAC
            compareHistory(); // Realizará as comparações com a lista global gerada nas outras execuções do código, dessa forma teremos um histórico
            try {
                saveInFile(); // Salva no arquivo histórico
            } catch (IOException ex) {
                Logger.getLogger(searchNetwork.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Varredura concluída!\n");
        }
    }

    public String getInitialIP(String ip) {
        // Captura os primeiros bytes do endereço IP a partir do IP do usuário
        String ipinicial = new String();
        for (int i = ip.length() - 1; i >= 0; --i) {
            if (ip.charAt(i) == '.') {
                ipinicial = ip.substring(0, i + 1);
                break;
            }
        }
        return ipinicial;
    }

    public void pingIPs(String ipinicial) {
        System.out.println("Começando a pingar os IPs, isso talvez demore um pouco, então para testes foi limitado para os primeiros 30 IPs!");
        // Percorre todos os IPs fazendo ping em cada um
        // Para questões de testes somente os IPs iniciais foram testados
        for (int i = 1; i <= 30; ++i) {
            try {
                InetAddress addr = InetAddress.getByName(ipinicial + Integer.toString(i));
                if (addr.isReachable(1000)) {
                    System.out.println("Pingando os IPs da rede...");
                }
            } catch (IOException io) {
                System.err.println(io);
            }
        }
    }

    public void arpTable() {
        try {
            Dispositivo dispositivo = new Dispositivo();
            String gateway = defineGateway(); // Define qual é o Gateway, para sabermos quem é router e quem é host
            InetAddress meuIp = InetAddress.getLocalHost();
            NetworkInterface meuNi = NetworkInterface.getByInetAddress(meuIp);

            // Gera a primeira entrada na lista, referente ao dispositivo do próprio usuário que está executando
            dispositivo.setIP(meuIp.getHostAddress());

            byte[] meuMac = meuNi.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < meuMac.length; i++) {
                sb.append(String.format("%02X%s", meuMac[i], (i < meuMac.length - 1) ? "-" : ""));
            }

            String macAddress = sb.toString();
            dispositivo.setMAC(macAddress);
            dispositivo.setFabricante(returnMACVendor(macAddress));
            dispositivo.setHorarioDescoberta(LocalTime.now());
            if ((dispositivo.getIP()).equals(gateway)) {
                dispositivo.setTipo("Roteador");
            } else {
                dispositivo.setTipo("Host");
            }
            dispositivo.setStatus("Novo");

            // Adiciona o dispositivo na lista de dispositivos conectados nessa "iteração"
            dispositivosConectados.add(dispositivo);

            // Executa o comando arp -a e faz parse dos dados para capturar todos dispositivos conectados
            Process proc = Runtime.getRuntime().exec("arp -a ");
            String tabela = new String();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            while ((tabela = stdInput.readLine()) != null) {
                String mac = "";
                String ip = "";
                Pattern patternIP = Pattern.compile(".*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
                Pattern patternMAC = Pattern.compile("\\s{0,}([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
                Matcher matcherIP = patternIP.matcher(tabela);
                Matcher matcherMAC = patternMAC.matcher(tabela);
                if (matcherMAC.find()) {
                    dispositivo = new Dispositivo();
                    if (matcherIP.find()) {
                        ip = mac + matcherIP.group().replaceAll("\\s", "");
                        dispositivo.setIP(ip);
                    }
                    mac = mac + matcherMAC.group().replaceAll("\\s", "");
                    dispositivo.setMAC(mac);
                    dispositivo.setFabricante(returnMACVendor(mac));
                    dispositivo.setHorarioDescoberta(LocalTime.now());
                    dispositivo.setTipo("N");
                    if ((dispositivo.getIP()).equals(gateway)) {
                        dispositivo.setTipo("Roteador");
                    } else {
                        dispositivo.setTipo("Host");
                    }
                    dispositivo.setStatus("Novo");
                    dispositivosConectados.add(dispositivo);
                }
            }
        } catch (IOException io) {
            System.err.println(io);
        }
    }

    public String returnMACVendor(String macAddress) throws FileNotFoundException, IOException {
        // Pega os primeiros bytes do endereço MAC e busca dentro do arquivo de fabricantes
        String vendorByte = getFirstBytes(macAddress);
        URL path = searchNetwork.class.getResource("mac-vendor.txt");
        File f1 = new File(path.getFile());
        String[] words = null;
        FileReader fr = new FileReader(f1);
        BufferedReader br = new BufferedReader(fr);
        String search;
        String vendor = new String();
        while ((search = br.readLine()) != null) {
            words = search.split("\t");
            if (Arrays.asList(words).contains(vendorByte.toUpperCase())) {
                return words[1];
            }
        }
        return "N/A";
    }

    public String getFirstBytes(String macAddress) {
        // Retorna somente os primeiros bytes do endereço MAC
        StringBuffer sb = new StringBuffer(macAddress.length());
        sb.setLength(macAddress.length());
        int index = 0;
        for (int i = 0; i < macAddress.length(); i++) {
            char cur = macAddress.charAt(i);
            if (cur != '-') {
                sb.setCharAt(index++, cur);
            }
        }
        return sb.toString().substring(0, 6);
    }

    public String defineGateway() {
        // Retorna o gateway/router
        try {
            Process result = Runtime.getRuntime().exec("netstat -rn");
            BufferedReader output = new BufferedReader(new InputStreamReader(result.getInputStream()));
            String line = output.readLine();
            while (line != null) {
                if (line.startsWith("Rotas ativas:") == true) {
                    line = output.readLine();
                    line = output.readLine();
                    break;
                }
                line = output.readLine();
            }
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            st.nextToken();
            return st.nextToken().toString();
        } catch (Exception e) {
        }
        return null;
    }

    public void compareHistory() {
        ArrayList<Dispositivo> compare = globalVariables.getHistory();
        if (compare == null) { // Se for a primeira execução
            globalVariables.setHistory(dispositivosConectados);
        } else {
            for (Dispositivo d : compare) {
                if (searchInsideNew(d.getMAC())) { // Verifica se o endereço MAC (único) está presente na lista da "iteração"
                    if ((d.getStatus()).equals("Removido")) { // Se estiver, verifica se estava com estado de removido
                        d.setStatus("Reativado"); // E adiciona o status reativado
                    } else { // Caso não estivesse removido, adiciona o estado antigo, para simbolizar que o dispositivo estava ativo e ainda está
                        d.setStatus("Antigo");
                    }
                } else {
                    d.setStatus("Removido"); // Caso o endereço MAC não estiver presente na nova lista de iteração, este foi removido
                }
            }

            for (Dispositivo ds : dispositivosConectados) {
                if (!searchInsideOld(ds.getMAC())) {
                    ds.setStatus("Novo"); // Caso o dispositivo ainda não existir na lista "global" ele é considerado novo
                    compare.add(ds);
                }
            }
        }
        globalVariables.setHistory(compare);
    }

    public boolean searchInsideOld(String mac) {
        for (Dispositivo ds : globalVariables.getHistory()) {
            if ((ds.getMAC()).equals(mac)) {
                return true;
            }
        }
        return false;
    }

    public boolean searchInsideNew(String mac) {
        for (Dispositivo ds : dispositivosConectados) {
            if ((ds.getMAC()).equals(mac)) {
                return true;
            }
        }
        return false;
    }

    public void saveInFile() throws IOException {
        System.out.println("Salvando resultados em 'historyDevices.txt'...");
        File f1 = new File("src/trabalhogerenciaredes/historyDevices.txt");
        f1.createNewFile();
        try (FileWriter tw = new FileWriter("src/trabalhogerenciaredes/historyDevices.txt",true)) {
            for (Dispositivo d : globalVariables.getHistory()) {
                tw.write(d.getIP() + " | " + d.getMAC() + " | " + d.getFabricante() + " | " + d.getHorarioDescoberta() + " | " + d.getStatus() + " | " + d.getTipo() + System.lineSeparator());
            }
            tw.write("Varredura realizada em " + LocalTime.now() + System.lineSeparator() + System.lineSeparator());
        }
    }
}
