/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.com.antenasur.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.el.ELContext;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author LEMAEDU
 */
public class JsfUtil implements Serializable {

    private static final long serialVersionUID = 1L;

    static final String GROWL_MESSAGES = "growlMessages";

    private static final String temporalTemp = System.getProperty("java.io.tmpdir") + File.separator;

    private static Random random = new Random();
    private static final int TAMANIO_PASSWORD = 8;

    /**
     * *********************************************************
     */
    public static ExternalContext getExternalContext() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }

    public static HttpSession geSession() {
        return (HttpSession) getExternalContext().getSession(true);
    }

    public static HttpServletResponse getHttpServletResponse() {
        return (HttpServletResponse) getExternalContext().getResponse();
    }

    /**
     * ******************************************************
     */
    public static String getPathHtmlReport(final String reporteHtml) {
        return getRequest().getSession().getServletContext().getRealPath("/htmlReport/" + reporteHtml);
    }

    /**
     * Obtiene un parámetro vía request por GET.
     *
     * @param key
     * @return
     */
    public static String getRequestParameter(String key) {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(key) == null ? ""
                : FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(key);
    }

    /**
     * Redirect a request to the specified URL, and cause the
     * responseComplete(), esa descripción lo dice todo!
     *
     * @param url
     * @throws RuntimeException, IOException
     */
    public static void redirect(String url) throws RuntimeException, IOException {
        FacesContext faces = FacesContext.getCurrentInstance();
        faces.getExternalContext().redirect(faces.getExternalContext().getRequestContextPath() + url);
    }

    public static HttpServletRequest getRequest() {
        return (HttpServletRequest) getExternalContext().getRequest();
    }

    /**
     * Obtiene la URL desde dónde viene la petición.
     *
     * @return
     */
    public static String getHttpReferer() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("referer");
    }

    /**
     * Obtiene pagina inicial
     *
     * @return
     */
    public static String getStartPage() {
        HttpServletRequest req = getRequest();
        String url = req.getRequestURL().toString();
        return url.substring(0, url.indexOf(req.getContextPath()) + req.getContextPath().length());
    }

    /**
     * Devuelve un objeto cargado a session
     *
     * @param nombre
     * @return object
     */
    public static Object devolverObjetoSession(final String nombre) {
        HttpServletRequest request = getRequest();
        return request.getSession().getAttribute(nombre);
    }

    /**
     * Devuelve un objeto cargado a session y lo elimina
     *
     * @param nombre
     * @return object
     */
    public static Object devolverEliminarObjetoSession(final String nombre) {
        HttpServletRequest request = getRequest();
        Object object = request.getSession().getAttribute(nombre);
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getSessionMap().remove(nombre);
        return object;
    }

    public static void eliminarObjetoSession(final String... nombre) {
        FacesContext context = FacesContext.getCurrentInstance();
        for (String s : nombre) {
            context.getExternalContext().getSessionMap().remove(s);
        }
    }

    /**
     * Carga objeto a session
     *
     * @param nombre
     * @param object
     */
    public static void cargarObjetoSession(final String nombre, final Object object) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getSessionMap().put(nombre, object);
    }

    /* M E N S A J E S */
    /**
     * Añade un mensaje de <code>error</code> a la pila de mensajes del
     * FacesContext.
     *
     * @param ex Excepción capturada, para mostrar el stacktrace, por ejemplo.
     * @param defaultMsg Mensaje general.
     */
    public static void addErrorMessage(Exception ex, String defaultMsg) {
        String msg = ex.getLocalizedMessage();
        if (msg != null && msg.length() > 0) {
            addErrorMessage(msg);
        } else {
            addErrorMessage(defaultMsg);
        }
    }

    /**
     * Añade mensajes de error en manera de bucle.
     *
     * @param messages
     */
    public static void addErrorMessages(List<String> messages) {
        for (String message : messages) {
            addErrorMessage(message);
        }
    }

    /**
     * Añade un mensaje de <code>error</code> a la pila de mensajes del
     * FacesContext.
     *
     * @param msg El mensaje en general.
     */
    public static void addErrorMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    /**
     * Muestra una lista de mensajes de error
     *
     * @param messages lista de mensajes
     */
    public static void addErrorMessage(List<String> messages) {

        for (String message : messages) {

            FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
            FacesContext.getCurrentInstance().addMessage(null, facesMsg);
        }
    }

    /**
     * Añade un mensaje de <code>información</code> a la pila de mensajes del
     * FacesContext.
     *
     * @param msg El mensaje en general.
     */
    public static void addInfoMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    public static void addInfoMessage(String msg, Boolean addInGrowl) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
        if (addInGrowl) {
            FacesContext.getCurrentInstance().addMessage(GROWL_MESSAGES, facesMsg);
        } else {
            FacesContext.getCurrentInstance().addMessage(null, facesMsg);
        }
    }

    /**
     * Añade un mensaje de info tipo: <code>FATAL</code> a la pila de mensajes
     * del FacesContext.
     *
     * @param msg
     */
    public static void addFatalMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_FATAL, msg, msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    /**
     * Añade un mensaje de info tipo: <code>WARNING</code> a la pila de mensajes
     * del FacesContext.
     *
     * @param msg
     */
    public static void addWarningMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_WARN, msg, msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMsg);
    }

    /**
     * Añade un mensaje de info tipo: <code>SUCCESS</code> a la pila de mensajes
     * del FacesContext.
     *
     * @param msg
     */
    public static void addSuccessMessage(String msg) {
        FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_INFO, msg, msg);
        FacesContext.getCurrentInstance().addMessage("successInfo", facesMsg);
    }

    /**
     * Valida RUC o Cedula
     *
     * @param numero
     * @return
     */
    public static boolean validarCedulaORUC(String numero) {
        int suma = 0;
        int residuo = 0;
        boolean privada = false;
        boolean publica = false;
        boolean natural = false;
        int numeroProvincias = 24;
        int digitoVerificador = 0;
        int modulo = 11;

        int d1, d2, d3, d4, d5, d6, d7, d8, d9, d10;
        int p1, p2, p3, p4, p5, p6, p7, p8, p9;

        d1 = d2 = d3 = d4 = d5 = d6 = d7 = d8 = d9 = d10 = 0;
        p1 = p2 = p3 = p4 = p5 = p6 = p7 = p8 = p9 = 0;

        if (numero.length() < 10) {
            return false;
        }

        // Los primeros dos digitos corresponden al codigo de la provincia
        int provincia = Integer.parseInt(numero.substring(0, 2));

        if (provincia <= 0 || provincia > numeroProvincias) {
            return false;
        }

        // Almacena los digitos de la cedula en variables.
        d1 = Integer.parseInt(numero.substring(0, 1));
        d2 = Integer.parseInt(numero.substring(1, 2));
        d3 = Integer.parseInt(numero.substring(2, 3));
        d4 = Integer.parseInt(numero.substring(3, 4));
        d5 = Integer.parseInt(numero.substring(4, 5));
        d6 = Integer.parseInt(numero.substring(5, 6));
        d7 = Integer.parseInt(numero.substring(6, 7));
        d8 = Integer.parseInt(numero.substring(7, 8));
        d9 = Integer.parseInt(numero.substring(8, 9));
        d10 = Integer.parseInt(numero.substring(9, 10));

        // El tercer digito es:
        // 9 para sociedades privadas y extranjeros
        // 6 para sociedades publicas
        // menor que 6 (0,1,2,3,4,5) para personas naturales
        if (d3 == 7 || d3 == 8) {
            return false;
        }

        // Solo para personas naturales (modulo 10)
        if (d3 < 6) {
            natural = true;
            modulo = 10;
            p1 = d1 * 2;
            if (p1 >= 10) {
                p1 -= 9;
            }
            p2 = d2 * 1;
            if (p2 >= 10) {
                p2 -= 9;
            }
            p3 = d3 * 2;
            if (p3 >= 10) {
                p3 -= 9;
            }
            p4 = d4 * 1;
            if (p4 >= 10) {
                p4 -= 9;
            }
            p5 = d5 * 2;
            if (p5 >= 10) {
                p5 -= 9;
            }
            p6 = d6 * 1;
            if (p6 >= 10) {
                p6 -= 9;
            }
            p7 = d7 * 2;
            if (p7 >= 10) {
                p7 -= 9;
            }
            p8 = d8 * 1;
            if (p8 >= 10) {
                p8 -= 9;
            }
            p9 = d9 * 2;
            if (p9 >= 10) {
                p9 -= 9;
            }
        }

        // Solo para sociedades publicas (modulo 11)
        // Aqui el digito verficador esta en la posicion 9, en las otras 2
        // en la pos. 10
        if (d3 == 6) {
            publica = true;
            p1 = d1 * 3;
            p2 = d2 * 2;
            p3 = d3 * 7;
            p4 = d4 * 6;
            p5 = d5 * 5;
            p6 = d6 * 4;
            p7 = d7 * 3;
            p8 = d8 * 2;
            p9 = 0;
        }

        /*
		 * Solo para entidades privadas (modulo 11)
         */
        if (d3 == 9) {
            privada = true;
            p1 = d1 * 4;
            p2 = d2 * 3;
            p3 = d3 * 2;
            p4 = d4 * 7;
            p5 = d5 * 6;
            p6 = d6 * 5;
            p7 = d7 * 4;
            p8 = d8 * 3;
            p9 = d9 * 2;
        }

        suma = p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
        residuo = suma % modulo;

        // Si residuo=0, dig.ver.=0, caso contrario 10 - residuo
        digitoVerificador = residuo == 0 ? 0 : modulo - residuo;
        int longitud = numero.length();
        // ahora comparamos el elemento de la posicion 10 con el dig. ver.
        if (publica) {
            if (digitoVerificador != d9) {
                return false;
            }
            /*
			 * El ruc de las empresas del sector publico terminan con 0001
             */
            if (!numero.substring(9, longitud).equals("0001")) {
                return false;
            }
        }

        if (privada) {
            if (digitoVerificador != d10) {
                return false;
            }
            if (!numero.substring(10, longitud).equals("001")) {
                return false;
            }
        }

        if (natural) {
            if (digitoVerificador != d10) {
                return false;
            }
            if (numero.length() > 10 && !numero.substring(10, longitud).equals("001")) {
                return false;
            }
        }
        return true;
    }

    public static boolean validarMail(String email) {
        boolean valido = false;
        Pattern patronEmail = Pattern.compile(
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
        Matcher mEmail = patronEmail.matcher(email);
        if (mEmail.matches()) {
            valido = true;
        }
        return valido;
    }

    /**
     * Obtener valor de un archivo .properties de la aplicacion
     *
     * @param key
     * @param isConfig Si es configuracion o Mensaje
     * @return
     */
    public static String getProperty(String key, boolean isConfig) {
        FacesContext context = FacesContext.getCurrentInstance();
        String fileProperties = isConfig ? "#{msg}" : "#{rpm}";
        ResourceBundle bundle = context.getApplication().evaluateExpressionGet(context, fileProperties,
                ResourceBundle.class);
        String value = bundle.getString(key);
        return value;
    }

    /**
     * Mes en letras
     *
     * @param month
     * @return
     */
    public static String mesText(int month) {
        String result = "";
        switch (month) {
            case 1: {
                result = "Enero";
                break;
            }
            case 2: {
                result = "Febrero";
                break;
            }
            case 3: {
                result = "Marzo";
                break;
            }
            case 4: {
                result = "Abril";
                break;
            }
            case 5: {
                result = "Mayo";
                break;
            }
            case 6: {
                result = "Junio";
                break;
            }
            case 7: {
                result = "Julio";
                break;
            }
            case 8: {
                result = "Agosto";
                break;
            }
            case 9: {
                result = "Septiembre";
                break;
            }
            case 10: {
                result = "Octubre";
                break;
            }
            case 11: {
                result = "Noviembre";
                break;
            }
            case 12: {
                result = "Diciembre";
                break;
            }
            default: {
                result = "Error";
                break;
            }
        }
        return result;
    }

    /**
     * Obtiene un managed bean del contexto
     *
     * @return managed bean registrado con name
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(Class<T> beanType) {
        String customName = null;
        try {
            customName = beanType.getAnnotation(ManagedBean.class).annotationType().getDeclaredMethod("name")
                    .invoke(beanType.getAnnotation(ManagedBean.class)).toString();
        } catch (Exception e) {

        }
        String standardBeanName = (beanType.getSimpleName().charAt(0) + "").toLowerCase()
                + beanType.getSimpleName().substring(1);

        if (customName != null && !customName.isEmpty()) {
            standardBeanName = customName;
        }

        ELContext elContext = FacesContext.getCurrentInstance().getELContext();
        return (T) FacesContext.getCurrentInstance().getApplication().getELResolver().getValue(elContext, null,
                standardBeanName);
    }

    /**
     *
     * <b> Obtiene el usuario autenticado, si no no ha iniciado session retorna
     * null. </b>
     *
     * @author Carlos Pupo
     * @version Revision: 1.0
     * <p>
     * [Autor: Carlos Pupo, Fecha: 30/01/2015]
     * </p>
     * @return
     */
    /*
	 * public static User getLoggedUser() { LoginBean instance =
	 * getBean(LoginBean.class); if (instance.getUser().getUserId()!=null) {
	 * //instance.getUser().setPassword(instance.getPassword()); return
	 * instance.getUser(); } return null; }
     */
    private static String devuelveDiaSemana(int dia) {
        switch (dia) {
            case 1:
                return "Domingo";
            case 2:
                return "Lunes";
            case 3:
                return "Martes";
            case 4:
                return "Miércoles";
            case 5:
                return "Jueves";
            case 6:
                return "Viernes";
            case 7:
                return "Sábado";
            default:
                return "";

        }
    }

    public static String getDateFormat(Date date) {
        Calendar fecha = Calendar.getInstance();
        fecha.setTime(date);
        return devuelveDiaSemana(fecha.get(Calendar.DAY_OF_WEEK)) + " " + fecha.get(Calendar.DAY_OF_MONTH) + " de "
                + mesText(fecha.get(Calendar.MONTH)) + " " + fecha.get(Calendar.YEAR);
    }

    public static String getDateFormatCalendar(Date date) {
        Calendar fecha = Calendar.getInstance();
        fecha.setTime(date);
        return devuelveDiaSemana(fecha.get(Calendar.DAY_OF_WEEK)) + " " + fecha.get(Calendar.DAY_OF_MONTH) + " de "
                + mesText(fecha.get(Calendar.MONTH) + 1) + " " + fecha.get(Calendar.YEAR);
    }

    public static String getIPAddress() {

        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getDate(Date date) {
        Calendar fecha = Calendar.getInstance();
        fecha.setTime(date);
        return fecha.get(Calendar.DAY_OF_MONTH) + " de " + mesText(fecha.get(Calendar.MONTH) + 1) + " de "
                + fecha.get(Calendar.YEAR);
    }

    public static String getFechaStringddMMYY(Date date) {
        String fechaString = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
        return fechaString.substring(0, 10);        
    }

    public static String getHoraStringHHmmss(Date date) {
        String fechaString = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
        return fechaString.substring(11, 19);        
    }

    public static String getFechaParaActas(Date date) {
        Calendar fecha = Calendar.getInstance();
        fecha.setTime(date);
        return "a los " + fecha.get(Calendar.DAY_OF_MONTH) + " días del mes de " + mesText(fecha.get(Calendar.MONTH) + 1) + " de "
                + fecha.get(Calendar.YEAR);
    }

    public static Timestamp getTimestamp() {
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        return ts;
    }

    /**
     * Devuelve una cadena encriptada en sha1
     *
     * @param password
     * @return
     */
    public static String claveEncriptadaSHA1(String password) {
        try {
            byte[] buffer = password.getBytes();
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(buffer);
            byte[] digest = md.digest();
            String valorHash = "";
            for (byte aux : digest) {
                int b = aux & 0xff;
                if (Integer.toHexString(b).length() == 1) {
                    valorHash += "0";
                }
                valorHash += Integer.toHexString(b);
            }
            return valorHash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Genera una cadena de caracteres aleatorea
     *
     * @param onlyChars
     * @return
     */
    public static synchronized String generatePassword(boolean... onlyChars) {
        String passwd = "";
        boolean isChar = onlyChars == null ? false : onlyChars.length > 0 ? onlyChars[0] : false;
        for (char c : complete("" + (int) (random.nextDouble() * 99999999), TAMANIO_PASSWORD, '0', true)
                .toCharArray()) {
            int value = (int) (Integer.parseInt("" + c) + Math.round(Math.random() * 120));
            char cc = (char) value;
            if (Character.isLetter(cc) & Character.isDefined(cc) & !Character.isWhitespace(cc)) {
                passwd += cc;
            } else {
                value = (int) (isChar ? Math.round(Math.random() * 25) + 65 : value);
                passwd += isChar ? (char) value : c;
            }
        }
        return passwd;
    }

    /**
     * Permite complementar una determinada cadena de texto con un caracter
     * especificado
     *
     * @param data Cadena de texto original
     * @param length longitud deseada
     * @param complete caracter con el cual se completara la cadena
     * @param reverse indica si la cadena se complementara al fina(false) o al
     * inicio(true)
     * @return cadena complementada, si la longitid es menor a la cadena
     * original se retornara la original sin ccambios
     */
    public static synchronized String complete(String data, final int length, final char complete,
            final boolean reverse) {
        final int size = data.length();
        StringBuilder build = new StringBuilder();
        if (reverse) {
            for (int i = size; i < length; i++) {
                build.append(complete);
            }
            build.append(data);
        } else {
            build.append(data);
            for (int i = size; i < length; i++) {
                build.append(complete);
            }
        }
        return build.toString();
    }

    public static synchronized Calendar getCalendarToDate(Date date) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        /*
		 * calendar.add(Calendar.YEAR, tiempo); Date date = calendar.getTime();
		 * SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd"); String date1 =
		 * format1.format(date); return date1;
         */
        return calendar;
    }

    /**
     * Valida si una pagina tiene permisos de acceso desde una session iniciada.
     *
     * @param pagina
     * @throws IOException
     * @throws RuntimeException
     */
    public static void validarPagina(final String pagina) throws RuntimeException, IOException {
        @SuppressWarnings("unchecked")
        List<String> listaPermisos = (List<String>) devolverObjetoSession("listaPermisos");
        if (listaPermisos != null && !listaPermisos.isEmpty()) {
            if (!listaPermisos.contains(pagina)) {
                redirect("/errors/permisos.jsf");
            }
        }
    }

    public static boolean validarContrasenia(final String clave) {
        boolean resultado = false;
        Integer cadenaPass = clave.length();
        if (cadenaPass > 7) {
            Pattern pat = Pattern.compile("^(?=\\w*\\d)(?=\\w*[A-Z])(?=\\w*[a-z])\\S{8,16}$");
            Matcher mat = pat.matcher(clave);
            if (mat.matches()) {
                resultado = true;
            } else {
                resultado = false;
                JsfUtil.addInfoMessage(
                        "Debe tener al menos un dígito, una minúscula, una mayúscula y un mínimo 8 caracteres");
            }
        } else {
            resultado = false;
            JsfUtil.addErrorMessage("Por favor ingresar mínimo 8 caracteres en el ingreso de la contraseña");
        }
        return resultado;
    }

    /**
     * Devuelve direción ip del servidor
     */
    public static String obtieneIpServidor() {
        InetAddress direccion;
        try {
            direccion = InetAddress.getLocalHost();
            String nombreDelHost = direccion.getHostName();// nombre host
            String IP_local = direccion.getHostAddress();// ip como String
            return IP_local;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }
    }

}
