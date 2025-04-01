SHOW COLUMNS FROM employee;

SELECT *
FROM dish
         LEFT JOIN category on dish.category_id = category.id
WHERE category.id = 11;

update dish
set image = 'https://seuer-sky-takeout.oss-cn-nanjing.aliyuncs.com/5f00088b-e42b-4594-9ee9-d9d7afc0a9d3.png';

# 营业额统计
select date(order_time) as date, sum(amount)
from orders
where date(order_time) between '2025-03-24' and '2025-3-31'
  and status = 5
group by date(order_time)
order by date(order_time);

# 新增用户统计
select date(create_time) as date,
       count(e1.id)      as newUser,
       (select count(id)
        from employee e2
        where date(e2.create_time) <= date(e1.create_time))
                         as totalUser
from employee e1
where date(create_time) between '2025-02-24' and '2025-3-31'
group by date(create_time)
order by date(create_time);

# 递归查询
# 使用with recursive table as递归查询相当于生成一张表table
with recursive date_list as (
    # 基础查询，返回结果2025-02-24并取别名为date
    select '2025-02-24' as date
    # 联合查询，会将下面的查询结果纵向拼到原始表中（即只有一条date数据的表）
    # union会去重，而union all不会去重
    union all
    # recursive会基于上一次查询的结果date然后查出一条新的日期
    # date_add用于日期增加
    select date_add(date, interval 1 day)
    from date_list
    # 知道当前日期等于2025-03-31停止
    # 这里不可以是<=，递归查询最后一个03-30，执行一次加1，即可得到03-31，所以03-31会被包括
    where date < '2025-03-31')
# coalesce判断表达式sum(orders.amount)是否为空，如果为空则返回0
# group by子句会根据date_list.date分组，如果某一天没有订单，则sum(orders.amount)为空，则coalesce会返回0

# coalesce函数属于select子句，会随select子句一起执行
# 而select子句在where子句之后执行，因此如果时间限制条件放在where中，在coalesce执行之前就已经被过滤掉了
select date_list.date                  as date,
       coalesce(sum(orders.amount), 0) as turnover
from date_list
         # left join会保留左表所有字段，即date_list的所有日期
         left join orders
    # on子句筛选连接条件
    # 先根据过滤条件orders.order_time between '2025-02-24' and '2025-03-31'筛选出范围内的数据
    # 然后根据连接条件进一步筛选

    # 可以不加时间限制条件，但是会导致左表的每一条数据都与整张右表匹配，开销大
    # 而添加更多的限制条件可以缩小右表，提升效率
                   on date_list.date = date(orders.order_time) and
                      date(orders.order_time) between '2025-02-24' and '2025-03-31'
# 这里不可以将orders.order_time between '2025-02-24' and '2025-03-31'放在where子句中
# where子句会在两张表连接完成后再对中间表进行过滤
# select子句在where子句之后执行，因此如果时间限制条件放在where中，在coalesce执行之前就已经被过滤掉了
# 此时大表中的有些order_time字段是空的（某些日期没有订单），而NULL与日期比较会返回false，因此这些行会直接被过滤掉
group by date_list.date
order by date_list.date;

# 递归查询
# 新增用户统计
with recursive date_list as (select '2025-03-01' as date
                             union all
                             select date_add(date, interval 1 day)
                             from date_list
                             where date < '2025-03-30')

select date_list.date                                 as date,
       coalesce(count(e1.id), 0)                      as newUser,
       (select count(e2.id)
        from employee e2
        where date(e2.create_time) <= date_list.date) as totalUser
from date_list
         left join employee e1
                   on date_list.date = date(e1.create_time) and
                      date(e1.create_time) between '2025-03-01' and '2025-03-30'
group by date_list.date
order by date_list.date;

# 订单查询
with recursive date_list as (select '2025-03-01' as date
                             union all
                             select date_add(date, interval 1 day)
                             from date_list
                             where date < '2025-03-30')

select date_list.date         as date,
       coalesce(count(id), 0) as orderCount
from date_list
         left join orders
                   on date_list.date = date(order_time)
                       and date(order_time) between '2025-03-01' and '2025-03-30'
                       and status = 5
group by date_list.date
order by date_list.date;

# top10
select name, sum(order_detail.number)  sales
from order_detail left join orders on
 order_detail.order_id = orders.id
and date(order_time) between '2025-03-01' and '2025-03-31'
group by name
order by sales desc
# 从0开始，取10条
limit 0, 10;


