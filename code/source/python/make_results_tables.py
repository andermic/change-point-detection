#! /usr/bin/python

# Some code for generating results tables in LaTex

ROOT_INPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/results/30hz'
OUTPUT_FOLDER = '/nfs/guille/wong/users/andermic/cpd/thesis'
METRICS = ['Accuracy', 'Detection Time']
EXPS = ['cpd','hmm']
DATASETS = [['OSU_YR4_Hip_30Hz.ws120.7cls', 'uq_30Hz_day2', 'uq_30Hz_day3'],['OSU_YR4_Hip_30Hz.HMM.7cls','uq_30Hz_day2.HMM','uq_30Hz_day3.HMM']]
DATASETS_DISP = ['OSU Hip', 'LiME Day 1', 'LiME Day 2']
CLASS_ALGS = ['dt','svm','nnet']
CPD_ALGS = ['cc', 'kliep']
FPRS = ['0.0005', '0.001', '0.005', '0.01']
FPRS_DISP = ['0.005', '0.01', '0.05', '0.1']
WSES = ['20', '10']

cpd = {}
hmm = {}
for dataset in DATASETS[0]:
    cpd[dataset] = {}
    for cls_alg in CLASS_ALGS:
        cpd[dataset][cls_alg] = {}
        for cpd_alg in CPD_ALGS:
            cpd[dataset][cls_alg][cpd_alg] = {}
            data = open('%s/cpd/%s/%s_%s_results.csv' % (ROOT_INPUT_FOLDER, dataset, cls_alg, cpd_alg)).readlines()[1:]
            data = [i.strip().split(',') for i in data]
            data = [i for i in data if i[0] in FPRS]
            for i_fpr in range(len(FPRS)):
                line = [i for i in data if i[0] == FPRS[i_fpr]][0]
                cpd[dataset][cls_alg][cpd_alg][FPRS_DISP[i_fpr]] = {}
                cpd[dataset][cls_alg][cpd_alg][FPRS_DISP[i_fpr]]['Accuracy'] = '%.1f' % (float(line[1]) * 100)
                cpd[dataset][cls_alg][cpd_alg][FPRS_DISP[i_fpr]]['SDAccuracy'] = '%.1f' % (float(line[2]) * 100)
                cpd[dataset][cls_alg][cpd_alg][FPRS_DISP[i_fpr]]['Detection Time'] = '%.1f' % float(line[3])
                cpd[dataset][cls_alg][cpd_alg][FPRS_DISP[i_fpr]]['SDDetection Time'] = '%.1f' % float(line[4])

for dataset in DATASETS[1]:
    hmm[dataset] = {}
    for cls_alg in CLASS_ALGS:
        hmm[dataset][cls_alg] = {}
        data = open('%s/hmm/%s/%s_results.csv' % (ROOT_INPUT_FOLDER, dataset, cls_alg)).readlines()[1:]
        data = [i.strip().split(',') for i in data]
        data = [i for i in data if i[0] in WSES]
        for ws in WSES:
            line = [i for i in data if i[0] == ws][0]
            hmm[dataset][cls_alg][ws] = {}
            hmm[dataset][cls_alg][ws]['Accuracy'] = '%.1f' % (float(line[1]) * 100)
            hmm[dataset][cls_alg][ws]['SDAccuracy'] = '%.1f' % (float(line[2]) * 100)
            hmm[dataset][cls_alg][ws]['Detection Time'] = '%.1f' % float(line[3])
            hmm[dataset][cls_alg][ws]['SDDetection Time'] = '%.1f' % float(line[4])

out = open('%s/tables.tex' % OUTPUT_FOLDER, 'w')

out.write('\\setlength{\\abovecaptionskip}{-5pt}\n\n')
for metric in METRICS:
 for i_exp in range(len(EXPS)):
    exp = EXPS[i_exp]
    sizes = FPRS if exp == 'cpd' else WSES
    sizes_disp = FPRS_DISP if exp == 'cpd' else WSES
    col_num = len(CLASS_ALGS) * len(sizes)
    xtra_cols = 2 if exp == 'cpd' else 1
    col_end = col_num + xtra_cols
    out.write('\\begin{table}[h]\n')
    out.write('\\captionsetup{font=scriptsize}\n')
    out.write('\\begin{center}\n')
    out.write('\\tiny{\n')
    out.write('\\begin{tabular}{ %s|%s }\n' % ('c' * xtra_cols, 'c|' * col_num))
    out.write('\\cline{%d-%d}\n' % (xtra_cols + 1, col_end))

    if exp == 'cpd': out.write('&')
    for class_alg in CLASS_ALGS:
        out.write(' & \\multicolumn{%d}{ c| }{%s}' % (len(sizes), class_alg.upper()))
    out.write('\\\\ \\cline{%d-%d}\n' % (xtra_cols + 1, col_end))

    if exp == 'cpd': out.write('&')
    for alg in CLASS_ALGS:
        for i_size in range(len(sizes)):
            out.write(' & %s' % sizes_disp[i_size])
    out.write('\\\\ \\cline{1-%d}\n' % col_end)

    for i_data in range(len(DATASETS[0])):
        if exp == 'cpd':
            out.write('\\multicolumn{1}{ |c| }{\\multirow{%d}{*}{%s}} &\n' % (len(CPD_ALGS), DATASETS_DISP[i_data]))
            for i_cpd in range(len(CPD_ALGS)):
                out.write('\\multicolumn{1}{ |c| }{%s}' % CPD_ALGS[i_cpd].upper())
                best = [-1 if metric == 'Accuracy' else 10**9,'','']
                for cls_alg in CLASS_ALGS:
                    for size_disp in sizes_disp:
                        cell_val = float(cpd[DATASETS[0][i_data]][cls_alg][CPD_ALGS[i_cpd]][size_disp][metric])
                        if metric == 'Accuracy':
                            if cell_val > best[0]:
                                best = [cell_val, cls_alg, size_disp]
                        else:
                            if cell_val < best[0]:
                                best = [cell_val, cls_alg, size_disp]
                for cls_alg in CLASS_ALGS:
                    for size_disp in sizes_disp:
                        cell_val = cpd[DATASETS[0][i_data]][cls_alg][CPD_ALGS[i_cpd]][size_disp][metric]
                        if cls_alg == best[1] and size_disp == best[2]:
                            out.write(' & \\textbf{%s}' % cell_val)
                        else:
                            out.write(' & %s' % cell_val)
                if i_cpd != len(CPD_ALGS) - 1:
                    out.write('\\\\ \\cline{2-%d}\n' % col_end)
                    out.write('\\multicolumn{1}{ |c }{} &\n')
                else:
                    out.write('\\\\ \\cline{1-%d}\n' % col_end)
        else:
            out.write('\\multicolumn{1}{ |c| }{%s}\n' % DATASETS_DISP[i_data])
            best = [-1 if metric == 'Accuracy' else 10**9,'','']
            for cls_alg in CLASS_ALGS:
                for size_disp in sizes_disp:
                    cell_val = float(hmm[DATASETS[1][i_data]][cls_alg][size_disp][metric])
                    if metric == 'Accuracy':
                        if cell_val > best[0]:
                            best = [cell_val, cls_alg, size_disp]
                    else:
                        if cell_val < best[0]:
                            best = [cell_val, cls_alg, size_disp]
            for cls_alg in CLASS_ALGS:
                for size_disp in sizes_disp:
                    cell_val = hmm[DATASETS[1][i_data]][cls_alg][size_disp][metric]
                    if cls_alg == best[1] and size_disp == best[2]:
                        out.write(' & \\textbf{%s}' % cell_val)
                    else:
                        out.write(' & %s' % cell_val)
            out.write('\\\\ \\cline{1-%d}\n' % col_end)
    out.write('\\end{tabular}\n')
    out.write('}\n')
    out.write('\\end{center}\n')
    out.write('\\caption{%s %s}\n' % (EXPS[i_exp].upper(), metric))
    out.write('\\label{tbl:%s_%s}\n' % (EXPS[i_exp], metric[:3].lower()))
    out.write('\\end{table}\n\n')

out.write('\\setlength{\\abovecaptionskip}{10pt}\n')
out.close()
