import cpu.CPU
import de.vandermeer.asciitable.AsciiTable
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.util.ioCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.io.readUTF8Line
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mem.Memory
import mem.MemoryPolicies
import java.net.InetSocketAddress
import java.net.ServerSocket
import javax.naming.SizeLimitExceededException
import kotlin.system.exitProcess

fun CPU.libres():String{
    var i=0
    var j=i

    var cadena = false
    var sString:String = ""
    var trigger = false
    this.swapMemory.pages.forEach {
        if(it.pid!=null){
            trigger = true
            if(cadena){
                cadena=false
                sString += "S["+i+"-"+j+":L],\n"
            }else {
                sString += "S[" + it.pageIndex + ":" + it.pid + "." + it.processPageIndex + "],\n"
            }
        }else{
            if(!cadena){
                i=it.pageIndex
                cadena=true
            }else{
                j++
            }
        }
    }
    if(!trigger){
        sString += "S[0"+"-"+j+":L],\n"
    }
    return sString
}



fun cuadro(tiempo:Int,comando:String,terminados:String,cpu:CPU,pid:Int):String{
    var mString:String = ""
    var sString:String = ""
    var at:AsciiTable = AsciiTable()
    at.addRule()
    at.addRow("Tiempo","Comando","dir. real","M","S","Terminados")
    at.addRule()
    cpu.realMemory.pages.forEach {
        if(it.pid==pid) {
            mString+="M["+it.pageIndex+":"+it.pid+"."+it.processPageIndex+"],\n"
        }
    }
    sString = cpu.libres()
    at.addRow(tiempo,comando,"",mString,sString,terminados)
    at.addRule()
    atFinal.addRow(tiempo,comando,"",mString,sString,terminados)
    atFinal.addRule()
    val rend:String = "\n"+at.render()
    return rend
}

fun CPU.acces(pdir:Int,pid:Int,modify:Boolean,comando: String,terminados: String,tiempo:Int):String{
    var at:AsciiTable = AsciiTable()
    at.addRule()
    at.addRow("Tiempo","Comando","dir. real","M","S","Terminados")
    at.addRule()
    val dir = this.accessProccess(pid,pdir,false)
    at.addRow(tiempo,comando,dir,"igual","igual",terminados)
    at.addRule()
    atFinal.addRow(tiempo,comando,dir,"igual","igual",terminados)
    atFinal.addRule()
    val rend:String = "\n"+at.render()
    return rend
}

fun CPU.liberar(pid:Int,tiempo:Int,comando:String,terminados: String):String{
    var at:AsciiTable = AsciiTable()
    var mString:String = ""
    var sString:String = ""
    this.realMemory.pages.forEach {
        if(it.pid==pid) {
            mString+="M["+it.pageIndex+":L],\n"
        }
    }
    this.swapMemory.pages.forEach {
        if(it.pid==pid) {
            sString+="S["+it.pageIndex+":L],\n"
        }
    }
    if(mString==""){mString="igual"}
    if(sString==""){sString="igual"}
    at.addRule()
    at.addRow("Tiempo","Comando","dir. real","M","S","Terminados")
    at.addRule()
    at.addRow(tiempo,comando,"",mString,sString,terminados)
    at.addRule()
    atFinal.addRow(tiempo,comando,"",mString,sString,terminados)
    atFinal.addRule()
    val rend:String = "\n"+at.render()
    this.clearProcess(pid = pid)
    return rend
}

fun CPU.fin(terminados: String,tiempo: Int):String{
    var at:AsciiTable = AsciiTable()
    at.addRule()
    at.addRow("Tiempo","Comando","dir. real","M","S","Terminados")
    at.addRule()
    at.addRow(tiempo,"F","","M[0-"+this.realMemory.size+":L]","S[0-"+this.swapMemory.size+":L]",terminados)
    at.addRule()
    atFinal.addRow(tiempo,"F","","M[0-"+this.realMemory.pages.size+":L]","S[0-"+this.swapMemory.pages.size+":L]",terminados)
    atFinal.addRule()
    val rend:String = "\n"+at.render()
    return rend
}

var atFinal:AsciiTable = AsciiTable()

fun main() {
    runBlocking {

        val server = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("127.0.0.1", 10001))
        println("Started echo telnet server at ${server.localAddress}")

        while (true) {
            val socket = server.accept()

            GlobalScope.launch {
                println("Socket accepted: ${socket.remoteAddress}")
                val input = socket.openReadChannel()
                val output = socket.openWriteChannel(autoFlush = true)
                try {
                    var realMem: Int = 0
                    var swapMem: Int = 0
                    var pageSize: Int = 0
                    while (true) {
                        if (realMem == 0 || swapMem == 0 || pageSize ==  0) {
                            var line = input.readUTF8Line()!!
                            if ("RealMemory" in line) {
                                line = line.removePrefix("RealMemory ")
                                realMem = line.toInt()
                                println("realMem=${realMem}")
                                output.writeStringUtf8("Memoria real = ${realMem}")
                            } else if ("SwapMemory" in line) {
                                line = line.removePrefix("SwapMemory ")
                                swapMem = line.toInt()
                                println("swapMem=${swapMem}")
                                output.writeStringUtf8("Memoria swap = ${swapMem}")
                            } else if ("PageSize" in line) {
                                line = line.removePrefix("PageSize ")
                                pageSize = line.toInt()
                                println("pageSize=${pageSize}")
                                output.writeStringUtf8("Tamaño de pagina = ${pageSize}")
                            } else {
                                output.writeStringUtf8("Comando no encontrado, porfavor use los siguientes comandos primero: Realmemory m, SwapMemory n, PageSize p, Donde m,n y p son numeros")
                            }
                        }
                        if (realMem > 0 && swapMem > 0 && pageSize > 0) {//ya se termino de inicializar
                            while (true) {
                                var check = true
                                var tiempo:Int = 0
                                atFinal.addRule()
                                atFinal.addRow("Tiempo","Comando","dir. real","M","S","Terminados")
                                atFinal.addRule()
                                val politica = input.readUTF8Line()!!
                                if ("FIFO" in politica) {
                                    output.writeStringUtf8("PoliticaMemory FIFO")
                                    val realMemory = Memory(realMem*Sizes.KB, pageSize, MemoryPolicies.FIFO)
                                    val swapMemory = Memory(swapMem*Sizes.KB, pageSize, MemoryPolicies.FIFO)

                                    val cpu = CPU(
                                            realMemory = realMemory,
                                            swapMemory = swapMemory,
                                            pageSize = pageSize
                                    )

                                    var terminados = ""//variable donde se guardan los procesos terminados
                                    while (check) {
                                        val comando = input.readUTF8Line()!!
                                        val arr = comando.split(" ")
                                        if(arr[0] == "P"){//nuevo proceso
                                            val pid = arr[2].toInt()
                                            val pSize = arr[1].toInt()
                                            cpu.spawnProcess(size = pSize, pid = pid)
                                            var rend = cuadro(tiempo,comando,terminados,cpu,pid)
                                            output.writeStringUtf8(comando)
                                            println(rend)
                                        }else if(arr[0] == "A") {//accesar proceso
                                            val pdir = arr[1].toInt()
                                            val pid = arr[2].toInt()
                                            val modify:Boolean = (arr[3]=="0")
                                            val result = cpu.acces(pdir,pid,modify,comando,terminados,tiempo)
                                            output.writeStringUtf8(comando)
                                            println(result)
                                        }else if(arr[0] == "L"){//liberar procesos
                                            val pid = arr[1].toInt()
                                            terminados+=""+pid+","
                                            val result = cpu.liberar(pid,tiempo,comando,terminados)
                                            println(result)
                                            output.writeStringUtf8(comando)
                                        }else if(arr[0] == "C"){//comentario
                                            val comentario = comando.removeSuffix("\r\n")
                                            output.writeStringUtf8(comentario)
                                        }else if(arr[0] == "F") {//fin. reinicia el programa y debe de poder seguir
                                            println(cpu.fin(terminados,tiempo))
                                            println("historial:")
                                            val rend:String = "\n"+atFinal.render()
                                            println(rend)
                                            output.writeStringUtf8(comando)
                                            check = false
                                        }else if(arr[0] == "E"){//terminar el programa
                                            output.writeStringUtf8("E\nGracias por usar el programa!")
                                            exitProcess(0)
                                        }
                                        tiempo++
                                    }
                                } else if ("LIFO" in politica) {
                                    output.writeStringUtf8("PoliticaMemory LIFO")
                                    //TODO, agregar inicializacion de cpu para LIFO
                                    val realMemory = Memory(realMem*Sizes.KB, pageSize, MemoryPolicies.LIFO)
                                    val swapMemory = Memory(swapMem*Sizes.KB, pageSize, MemoryPolicies.LIFO)

                                    val cpu = CPU(
                                            pageSize = pageSize,
                                            realMemory = realMemory,
                                            swapMemory = swapMemory
                                    )
                                    var terminados = ""//variable donde se guardan los procesos terminados
                                    while (check) {
                                        val comando = input.readUTF8Line()!!
                                        val arr = comando.split(" ")
                                        if(arr[0] == "P"){//nuevo proceso
                                            val pid = arr[2].toInt()
                                            val pSize = arr[1].toInt()
                                            cpu.spawnProcess(size = pSize, pid = pid)
                                            var rend = cuadro(tiempo,comando,terminados,cpu,pid)
                                            output.writeStringUtf8(comando)
                                            println(rend)
                                        }else if(arr[0] == "A") {//accesar proceso
                                            val pdir = arr[1].toInt()
                                            val pid = arr[2].toInt()
                                            val modify:Boolean = (arr[3]=="0")
                                            val result = cpu.acces(pdir,pid,modify,comando,terminados,tiempo)
                                            output.writeStringUtf8(comando)
                                            println(result)
                                        }else if(arr[0] == "L"){//liberar procesos
                                            val pid = arr[1].toInt()
                                            terminados+=""+pid+","
                                            val result = cpu.liberar(pid,tiempo,comando,terminados)
                                            println(result)
                                            output.writeStringUtf8(comando)
                                        }else if(arr[0] == "C"){//comentario
                                            val comentario = comando.removeSuffix("\r\n")
                                            output.writeStringUtf8(comentario)
                                        }else if(arr[0] == "F") {//fin. reinicia el programa y debe de poder seguir
                                            println(cpu.fin(terminados,tiempo))
                                            println("historial:")
                                            val rend:String = "\n"+atFinal.render()
                                            println(rend)
                                            output.writeStringUtf8(comando)
                                            check = false
                                        }else if(arr[0] == "E"){//terminar el programa
                                            output.writeStringUtf8("E\nGracias por usar el programa!")
                                            exitProcess(0)
                                        }
                                        tiempo++
                                    }
                                }else if(politica == "E"){
                                    output.writeStringUtf8("E\nGracias por usar el programa!")
                                    exitProcess(0)
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {

                }
            }
        }
    }
}