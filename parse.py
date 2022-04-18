import pydot
import pprint as pp


file = '/home/kosho/spatial/gen/Tst_pirTest/Pixelfly_test_512_2_128/pir/out/dot/global.dot'


unit_type_of_interest = "ComputeContainer"
cu_prefix = "ComputeContainer"
src_ctx_tag = "srcCtx"
target_fname = "Pixelfly_test.scala"
graphs = pydot.graph_from_dot_file(file)
cu_graph = graphs[0]
nodes = cu_graph.get_nodes()
lino_cu_list_dict = {}
    
for _ in nodes:
    uname = _.get_name()
    
    if unit_type_of_interest in uname:
        src_ctx_str_list = filter(lambda x: 'srcCtx' in x, _.get_attributes()['tooltip'].split('\n'))
        
        tmp = []
        for _s in src_ctx_str_list:
            scala_lino_list = _s.split('List')[-1][1:-1].split(', ')
            
            for _ss in scala_lino_list:
                if target_fname in _ss:
                    tmp.append(_ss)
            tmp = sorted(tmp, key=lambda x: int(x[x.find(':')+1 : x.rfind(':')]))  
        
        
        if len(tmp) > 0:
            if tmp[0] not in lino_cu_list_dict:
                lino_cu_list_dict[tmp[0]] = []
            lino_cu_list_dict[tmp[0]].append(uname)

cnt_n_v = 0
for k in sorted(lino_cu_list_dict.keys()):
    lv = lino_cu_list_dict[k]
    html_str = ['C' + _[len(cu_prefix):] for _ in lv]
    n_v = len(lv)
    print(k, n_v, html_str)
    cnt_n_v += n_v  
print(cnt_n_v, 'CUs')









print('\n\n\n')












unit_type_of_interest = "MemoryContainer"
cu_prefix = "MemoryContainer"
src_ctx_tag = "srcCtx"
target_fname = "Pixelfly_test.scala"
graphs = pydot.graph_from_dot_file(file)
cu_graph = graphs[0]
nodes = cu_graph.get_nodes()
lino_cu_list_dict = {}
    
for _ in nodes:
    uname = _.get_name()
    
    if unit_type_of_interest in uname:
        src_ctx_str_list = filter(lambda x: 'srcCtx' in x, _.get_attributes()['tooltip'].split('\n'))
        
        tmp = []
        for _s in src_ctx_str_list:
            scala_lino_list = _s.split('List')[-1][1:-1].split(', ')
            
            for _ss in scala_lino_list:
                if target_fname in _ss:
                    tmp.append(_ss)
            tmp = sorted(tmp, key=lambda x: int(x[x.find(':')+1 : x.rfind(':')]))
        
        if len(tmp) > 0:
            if tmp[0] not in lino_cu_list_dict:
                lino_cu_list_dict[tmp[0]] = []
            lino_cu_list_dict[tmp[0]].append(uname)

cnt_n_v = 0
for k in sorted(lino_cu_list_dict.keys()):
    lv = lino_cu_list_dict[k]
    html_str = ['M' + _[len(cu_prefix):] for _ in lv]
    n_v = len(lv)
    print(k, n_v, html_str)
    cnt_n_v += n_v  
print(cnt_n_v, 'MUs')