package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import kotlin.system.exitProcess

val jsonFile = File("tasklist.json")
val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)!!
val jsonAdapter: JsonAdapter<MutableList<Task>> = moshi.adapter(type)

fun main() = TaskList().runApp()

data class Task(
    var id: Int,
    var priority: String,
    var date: List<Int>,
    var time: List<Int>,
    var tasks: MutableList<String>
)

class TaskList {
    fun runApp(): Unit {
        return if (jsonFile.exists()) {
            jsonAdapter.fromJson(jsonFile.readText())!!.actions()
        } else mutableListOf<Task>().actions()
    }

    private fun MutableList<Task>.actions() {
        println("Input an action (add, print, edit, delete, end):")
        when (readln().lowercase()) {
            "add" -> addTask()
            "print" -> printTaskList()
            "edit" -> printTaskList(). also { editTask() }
            "delete" -> printTaskList(). also { deleteTask() }
            "end" -> endProgram()
            else -> println("The input action is invalid")
        }
        actions()
    }

    private fun MutableList<Task>.addTask() {
        val newTask = createClass(this.size)
        if (newTask!!.tasks.isNotEmpty()) add(newTask)
    }

    private fun MutableList<Task>.printTaskList() {
        if (this.size == 0) println("No tasks have been input").also { actions() }
        println(
            """
                +----+------------+-------+---+---+--------------------------------------------+
                | N  |    Date    | Time  | P | D |                   Task                     |
                +----+------------+-------+---+---+--------------------------------------------+
            """.trimIndent()
        )
        for (i in 0 until this.size) {
            with(this[i]) {
                println("| ${String.format("%-2d", i + 1)} " +
                        "| ${LocalDateTime(date[0], date[1], date[2], time[0], time[1]).toString()
                            .replace("T", " | ")} " +
                        "| ${setPriorityColor(priority)} | ${setDueTag(date[0], date[1], date[2])} |${tasks[0]}|")
                if (tasks.size > 1)
                    for (j in 1 until tasks.size) {
                        println("|    |            |       |   |   |${tasks[j]}|")
                    }
                println("+----+------------+-------+---+---+${"-".repeat(44)}+")
            }
        }
    }

    private fun MutableList<Task>.editTask() {
        val input = this[getTaskIndex()]
        while (true) {
            println("Input a field to edit (priority, date, time, task):")
            when (readln()) {
                "priority" -> input.priority = setPriority()
                "date" -> input.date = setDate()
                "time" -> input.time = setTime()
                "task" -> input.tasks = setTasks()
                else -> {
                    println("Invalid field")
                    continue
                }
            }
            println("The task is changed").also { return }
        }
    }

    private fun MutableList<Task>.deleteTask() {
        val input = getTaskIndex()
        removeAt(input)
        println("The task is deleted").also { return }
    }

    private fun MutableList<Task>.endProgram() {
        if (!jsonFile.exists()) jsonFile.createNewFile()
        jsonFile.writeText(jsonAdapter.toJson(this))
        println("Tasklist exiting!").also { exitProcess(0) }
    }

    private fun MutableList<Task>.getTaskIndex(): Int {
        while (true) {
            println("Input the task number (1-${size}):")
            val input = readln()
            if (input !in "1"..size.toString()) {
                println("Invalid task number")
                continue
            }
            return input.toInt()-1
        }
    }

    private fun createClass(input: Int): Task? {
        val priority = setPriority()
        val date = setDate()
        val time = setTime()
        val tasks = setTasks()
        return Task(input, priority, date, time, tasks)
    }
}

fun setPriority(): String {
    println("Input the task priority (C, H, N, L):")
    val priorityChoice = arrayOf("C", "H", "N", "L")
    val input = readln().uppercase()
    return if (priorityChoice.contains(input)) input else setPriority()
}

fun setDate(): List<Int> {
    println("Input the date (yyyy-mm-dd):")
    return try {
        val input = readln().split("-").map { it.toInt() }
        LocalDate(input[0], input[1], input[2])
        return input
    } catch (e: Exception) {
        println("The input date is invalid").run { setDate() }
    }
}

fun setTime(): List<Int> {
    println("Input the time (hh:mm):")
    return try {
        val input = readln().split(":").map { it.toInt() }
        LocalDateTime(2020,1,1,input[0], input[1])
        return input
    } catch (e: Exception) {
        println("The input time is invalid").run { setTime() }
    }
}

fun setTasks() = mutableListOf<String>().also { it.addTasks() }

private fun MutableList<String>.addTasks() {
    ifEmpty { println("Input a new task (enter a blank line to end):") }
    val input = readln().trim().chunked(44).toMutableList()
    for (i in input.indices) input[i] = input[i].padEnd(44, ' ')
    input.let { taskList ->
        if (taskList.isNotEmpty()) taskList.forEach { add(it) }.run { addTasks() }
        else if (isNotEmpty() && taskList.isEmpty()) return
        else println("The task is blank")
    }
}

fun setPriorityColor(priority: String): String {
    return when (priority) {
        "C" -> "\u001B[101m \u001B[0m"
        "H" -> "\u001B[103m \u001B[0m"
        "N" -> "\u001B[102m \u001B[0m"
        else -> "\u001B[104m \u001B[0m"
    }
}

fun setDueTag(year: Int, month: Int, day: Int): String {
    val taskDate = LocalDate(year, month, day)
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+2")).date
    val numDays = currentDate.daysUntil(taskDate)
    return when {
        numDays < 0 -> "\u001B[101m \u001B[0m"
        numDays > 0 -> "\u001B[102m \u001B[0m"
        else -> "\u001B[103m \u001B[0m"
    }
}
