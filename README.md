# ycup2021

Результат - 3 место.
Итоги забавные - самые сложные задачи были решены хорошо, а самые простые задачи, видимо, плохо - и, в итоге, не удовлетворили тестировщиков.
Задачи B и С решены хорошо (80-100 %); A D E - (0-40%) - думаю, все дело в том, что недостаточно потестировал сам перед отправкой, т.к. само направление решений оказалось верным


![ycup_results](https://user-images.githubusercontent.com/9281221/139576264-1545b69c-c0d8-4f9d-98ab-a80f6fcd3dba.png)


## A. Уроки Йоги

Алёна очень любит заниматься йогой, но постоянно забывает дышать правильно.  Напишите приложение, которое будет определять с помощью встроенного микрофона, когда она делает вдох, когда — выдох, а когда вообще задержала дыхание, чтобы ее тренер Аркадий смог помочь ей тренировать дыхание.

Решение должно определять вдох, выдох и время между ними.

Приложение должно:

- определять момент вдоха,
- определять момент выдоха,
- считать и фиксировать время между вдохом-выдохом и выдохом-вдохом,
- составлять список вдохов-выдохов с зафиксированным временем между ними,
- отправлять список по электронной почте тренеру Аркадию.



## B. "Войнушки" возвращаются

В детстве все любили играть в "войнушку", главной проблемой которой было выяснить, кто в кого первый попал. Благодаря современным технологиям "войнушку" можно модифицировать. Вместо игрушечных пистолетов в нее можно играть телефонами: увидел товарища, направил на него телефон, ткнул в экран — кто успел первым, тот и молодец. Можно вести счет, устраивать чемпионаты и даже делиться результатами в соцсетях, чтобы все точно знали, кто выиграл.

Вам предстоит написать приложение, позволяющее играть в "войнушку" "по сети".

**Приложение должно:**

- вести «сессию» на участников игры в войнушку. Приложение знает других участников, их координаты и другие параметры,
- уметь «стрелять». «Выстрел» — это касание экрана в направлении одного из играющих. Приложение оценивает параметры и регистрирует попадание (можно с некоторым эпсилон),
- вести статистику «живых» игроков, регистрировать победителя,
- показывать на экране поток с камеры (AR). Для полного балла по задаче при попадании в поле зрения экрана надо показывать метку игрока (любую на усмотрение участника).



## C. Карта покрытия

Игорь в своей комнате страдает от того, что где бы он не сел, сигнал Wi-Fi от его роутера недостаточный, чтобы он мог смотреть свои любимые видеоролики на своем любимом сайте. Давайте поможем Игорю.

Для этого вам необходимо разработать приложение, которое просит пройтись сначала по периметру комнаты, а потом по ее площади (каким либо способом, но по всей) и составляет карту покрытия комнаты Wi-Fi сигналом.  Таким образом мы сможем помочь Игорю определить лучшую точку для организации рабочего места и выбрать точку с самым слабым сигналом для установления второй точки доступа в интернет.

**Приложение должно:**

- построить и отобразить тепловую карту помещения по уровню приема Wi-Fi-сигнала,
- попросить пользователя пройти по периметру комнаты, а потом по площади и собрать уровень сигнала.
- регистрировать препятствия на карте (пользователь прошел по площади комнаты, но «посетил» не все места).



## D. Утренняя зарядка

Даша знает, что зарядка очень полезна, и выполняет ее исправно каждый день. Иногда Даше кажется, что она делает зарядку недостаточно энергично, а порой она не успевает прийти в себя после сна. Тогда она не может придумать, какие упражнения ей нужно делать. Помогите Даше — напишите приложение, которое подсказывает упражнения и определяет, насколько качественно оно было выполнено.

Начать предлагаем с планки, необходимо посчитать время, которое Даша стоит в планке.

**Приложение должно:**

- фиксировать визуально или через акселерометр начало и конец упражнения «планка»,
- фиксировать время стояния "в планке",
- вести учет попыток и показывать их в списке с возможностью добавить новую попытку.



## E. Спасаем Техас

Вы оказались в Техасе после очередного сильного урагана. Администрация штата поручила вам разработать приложение для инвентаризации инфраструктуры. Напишите приложение для коммунальщиков, которые на местности смогут отмечать следующие уничтоженные объекты:

- столбы ЛЭП;
- уличные фонари;
- деревья;
- почтовые ящики;
- пожарные гидранты.


Поскольку сотовая связь порядком пострадала, нужно хранить данные офлайн и отправлять их по электронной почте, когда появляется интернет.

Север готов принимать запросы в формате:

{
    "coordinates": {
        "lat": float,
        "lon": float
    },
    "objects": {
        "object": {
            "type": "<power_pylon | streetlight | tree | mailbox | hydrant>"
            "count": int        
        }
    }
}

Приложение должно:

- задавать местоположение объекта на карте или определять его автоматически,
- позволять выбирать тип объекта,
- собирать данные при отсутствии сети и восстанавливаться после своей перезагрузки,
- отправлять собранные данные по электронному адресу, указанному пользователем.
