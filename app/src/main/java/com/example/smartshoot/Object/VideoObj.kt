package com.example.smartshoot.Object

class VideoObj(name : String , date : String , time : String , photo : String) {

        var name : String = ""
            get() = field
            set(value) {
                field = value
            }

        var date : String = ""
            get() = field
            set(value) {
                field = value
            }

        var time : String = ""
            get() = field
            set(value) {
                field = value
            }

        var photo : String = ""
            get() = field
            set(value) {
                field = value
            }

        init {
            this.name = name
            this.date = date
            this.time = time
            this.photo = photo
        }

}