### Комментарий
Для распознавания звука дыхания Используется модель YAMNet в потоковом режиме
и небольшая фильтрация выходных данных модели для разграничения этапов вдоха и выдоха
к сожалению, не смог пока дообучить ее отличать имеено вдох от выдоха
поэтому в этой первой версии я просто допускаю, что сначала человек вдыхает, а затем идет непрерывная последовательность выдох-вдох-выдох
также отмечу, что фильтр настроен на медленное дыхание (в йоге, думаю, не очень активно дышат)

Возможные доработки:
- дообучение модели для отличия вдохов и выдохов
- адаптивный фильтр для любой скорости дыхания