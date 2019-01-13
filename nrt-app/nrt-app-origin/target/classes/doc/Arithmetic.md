<script type="text/javascript" src="http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=default"></script>
# Cid3 preference:
衰减系数:$$ \beta_T = e^{\alpha T} $$
T为距离当前日期的天数, \\(\alpha\\)目前固定为-0.1,后续可调整  
某个cid3的得分:$$score = \frac{\beta_TN_T+\beta_{T-1}N_{T-1}\cdots+\beta_{T-n}N_{T-n}}{\beta_TT_T+\beta_{T-1}T_{T-1}\cdots+\beta_{T-n
}T_{T-n}} $$
其中\\(N_T\\)为T日当天此cid3的点击数,\\(T_T\\)为T日当天的点击总数,\\(\beta_T\\)为T日的衰减系数  
对于一个cid3而言,当天时间范围内,之前的每天点击数和每天的衰减系数是固定的,因此可以作为一个参数保存,每天更新即可:
$$preScore = \beta_{T-1}N_{T-1}\cdots+\beta_{T-7}N_{T-7} $$

# Related Cid3 weight:
查询用户7天点击行为sku的cid3,处理得到cid3->count的map:
>$$ Query(Set_{sku}) = M_{(cid3,c)} $$

简单归一化得到cid3的浏览占比权重(最后结果需要截断控制长度):
>$$ Normal(M_{(cid3,c)}) = M_{(c3,w1)} $$

获取cid3的相关cid3以及相关权重：
>$$ Relate(M_{(c3,w1)}) = MR_{(c3,(c3,w2))} $$

根据占比权重和相关权重计算相关cid3的权重（排除浏览的cid3，最后结果根据权重降序并截断控制长度）:
>$$ RW(M_{(c3,w1)}, MR_{(c3,(c3,w2))}) = MR'_{(c3,w3)} $$
权重计算公式：$$W(w_1...w_n,w_1'...w_n') = Max(w_k * w_k')$$
w_k为对应浏览cid3的浏览权重，w_k'为相关权重，某个相关cid3的权重为浏览权重*相关权重，当存在多个得分时取大的