/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabalhogerenciaredes;

import java.time.LocalTime;

/**
 *
 * @author Otavio
 */
public class Dispositivo {
    String IP, MAC, fabricante, tipo, status;
    LocalTime horarioDescoberta;

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public LocalTime getHorarioDescoberta() {
        return horarioDescoberta;
    }

    public void setHorarioDescoberta(LocalTime horarioDescoberta) {
        this.horarioDescoberta = horarioDescoberta;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
