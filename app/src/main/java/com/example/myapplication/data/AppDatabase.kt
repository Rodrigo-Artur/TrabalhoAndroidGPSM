@Database(entities = [Contacto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactoDao(): ContactoDao
}