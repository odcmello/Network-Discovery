/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhogerenciaredes;

import java.util.ArrayList;

/**
 *
 * @author Otavio
 */
class globalVariables {
    
    // Nessa lista será mantido o contexto histórico do detector de dispositivos
    // Em cada execução do código de busca na rede, uma lista será gerada e comparada com esta
    public static ArrayList<Dispositivo> dispositivosConectadosGlobal = new ArrayList();

    static void setHistory(ArrayList<Dispositivo> setter) {
        dispositivosConectadosGlobal = setter;
    }
    
    static ArrayList<Dispositivo> getHistory(){
        return dispositivosConectadosGlobal;
    }
}
