# Homework #1
Учащийся - Латышев Евгений Сергеевич, MADE-31
## Beginner
1. Пробросить порт (port forwarding) для доступа к HDFS Web UI

```ssh -i emr.pem -N -L 5752:ec2-3-249-21-2.eu-west-1.compute.amazonaws.com:50070 hadoop@ec2-3-249-21-2.eu-west-1.compute.amazonaws.com```

2. [3 балла] Воспользоваться Web UI для того, чтобы найти папку “/data” в HDFS. Сколько подпапок в указанной папке /data?

1 подпапка — texts

## Intermediate
#### См. флаг “-ls” , чтобы:
1. [3 балла] Вывести список всех файлов в /data/texts

```hdfs dfs -ls /data/texts```

2. [3 балла] См. п.1 + вывести размер файлов в “human readable” формате (т.е. не в байтах, а например в МБ, когда
размер файла измеряется от 1 до 1024 МБ).

```hdfs dfs -ls -h /data/texts```

3. [3 балла] Команда "hdfs dfs -ls" выводит актуальный размер файла (actual) или же объем пространства, занимаемый с
учетом всех реплик этого файла (total)? В ответе ожидается одно слово: actual или total.

actual

#### С м . флаг “-du“:
1. [3 балла] Приведите команду для получения размера пространства, занимаемого всеми файлами внутри
“/data/texts”. На выходе ожидается одна строка с указанием команды.

```hdfs dfs -du -s -h /data/texts```

#### См. флаги “-mkdir” и “-touchz“:
1. [4 балла] Создайте папку в корневой HDFS-папке Вашего пользователя

```hdfs dfs -mkdir /latyshev```

2. [4 балла] Создайте в созданной папке новую вложенную папку.

```hdfs dfs -mkdir /latyshev/inner```

3. [4 балла] Что такое Trash в распределенной FS? Как сделать так, чтобы файлы удалялись сразу, минуя “Trash”?

Для предотвращения случайного удаления файлов, при удалении файла из HDFS он не удаляется сразу, а переносится в специальную директорию .Trash, которая очищается по расписанию. Удалить файл минуя .Trash можно с помощью флага -skipTrash

4. [4 балла] Создайте пустой файл в подпапке из пункта 2.

```hdfs dfs -touchz /latyshev/inner/tmp.txt```

5. [3 балла] Удалите созданный файл.

```hdfs dfs -rm -skipTrash /latyshev/inner/tmp.txt```

6. [3 балла] Удалите созданные папки.

```hdfs dfs -rm -r -skipTrash /latyshev```

#### См. флаги “-put”, “-cat”, “-tail”, “-distcp”:
1. [4 балла] Используя команду “-distcp” скопируйте рассказ О’Генри “Дары Волхвов” henry.txt из
  s3://texts-bucket/henry.txt в новую папку на HDFS

```hadoop distcp s3://texts-bucket/henry.txt /latyshev/dir1```

2. [4 балла] Выведите содержимое HDFS-файла на экран.

```hdfs dfs -cat /latyshev/dir1/henry.txt```

3. [4 балла] Выведите содержимое нескольких последних строчек HDFS-файла на экран.

```hdfs dfs -tail /latyshev/dir1/henry.txt```

4. [4 балла] Выведите содержимое нескольких первых строчек HDFS-файла на экран.

```hdfs dfs -cat /latyshev/dir1/henry.txt | head```

В Hadoop 3 добавили опцию -head и можно обойтись ```hdfs dfs -head /latyshev/dir1/henry.txt```

5. [4 балла] Переместите копию файла в HDFS на новую локацию.

```hdfs dfs -mv /latyshev/dir1/henry.txt /latyshev/dir2/henry.txt```

## Advanced
Полезные флаги:
● Для “hdfs dfs”, см. “-setrep -w”
● hdfs fsck /path -files - blocks -locations
1. ?
2. [6 баллов] Изменить replication factor для файла. Как долго занимает время на увеличение /
уменьшение числа реплик для файла?

15 секунд на выполнение ```hdfs dfs -setrep -w 2 /latyshev/dir2/henry.txt```
17 секунд на выполнение ```hdfs dfs -setrep -w 1 /latyshev/dir2/henry.txt```

3. [6 баллов] Найдите информацию по файлу, блокам и их расположениям с помощью “hdfs fsck”

```hdfs fsck /latyshev/dir2/henry.txt -files -blocks -locations```

4. [6 баллов] Получите информацию по любому блоку из п.2 с помощью "hdfs fsck -blockId”.
Обратите внимание на Generation Stamp (GS number).

```hdfs fsck -blockId blk_1073745766```
