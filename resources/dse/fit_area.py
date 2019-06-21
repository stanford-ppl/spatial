import pandas as pd
import numpy as np
import time
import sklearn
import pickle

from collections import namedtuple
from sklearn.linear_model import LinearRegression, Ridge
from sklearn import ensemble
from sklearn.neural_network import MLPRegressor
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
from sklearn.preprocessing import PolynomialFeatures, StandardScaler
from sklearn.pipeline import make_pipeline

import xgboost as xgb

# This is for querying the estimated area of a given spatial node

class ReportStats(namedtuple('ReportStats', ['mse', 'mae', 'r2', 'abs_error', 'rel_error', \
                                             'failed_node_names', 'failed_node_gold', 'failed_node_pred'
                                             ])):
    pass


class IRTrainingSession(namedtuple('IRTrainingSession', ['node_type', 'input_feature_list'])):
    pass


class ModelMetaData(namedtuple('ModelMetaData', ['best_model_name', 'best_mae', 'model_pickle', 'prediction_task'])):
    pass


XilinxFPGAResourceTargets = [
    'LUTs', 'FFs', 'SRLs', 'RAMB32', 'RAMB18', 'DSPs'
]

type = "par"
input_file = "regression_full." + type + ".csv"
df = pd.read_csv(input_file, header=0)
trained_model_dir = "./trained_models/"
df.sort_values(by='nodetype', axis=0, inplace=True)
names = df['nodetype'].unique().tolist()
subframes = [df.loc[df.nodetype == name].dropna(axis=1, how='all') for name in names]

ensemble_params = {
    "n_estimators": 500,
    "max_depth": 4,
    "min_samples_split": 2,
    "learning_rate": 0.01,
    "loss": "ls"
}
model_zoo = [
    #     (
    #         'PolynomialFeatures(1)+xgb.XGBRegressor',
    #         make_pipeline(PolynomialFeatures(1), xgb.XGBRegressor(objective="reg:linear", random_state=42))
    #     ),
    #     (
    #         'PolynomialFeatures(2)+xgb.XGBRegressor',
    #         make_pipeline(PolynomialFeatures(2), xgb.XGBRegressor(objective="reg:linear", random_state=42))
    #     ),
    #     (
    #         'LinearRegression',
    #         make_pipeline(LinearRegression())
    #     ),

    #     (
    #         'PolynomialFeatures(2)+Ridge',
    #         make_pipeline(PolynomialFeatures(2), Ridge())
    #     ),
    #     (
    #         'StandardScaler+MLPRegressor',
    #         make_pipeline(StandardScaler(), MLPRegressor(hidden_layer_sizes=(128,), max_iter=10000))
    #     ),
    #     (
    #         'PolynomialFeatures(2)+MLPRegressor',
    #         make_pipeline(PolynomialFeatures(2), MLPRegressor(hidden_layer_sizes=(256,), max_iter=10000))
    #     ),
    #     (
    #         'MLPRegressor',
    #         make_pipeline(MLPRegressor(hidden_layer_sizes=(128,), max_iter=10000))
    #     ),
    #     (
    #         'MLPRegressor(100, 100)',
    #         make_pipeline(MLPRegressor(hidden_layer_sizes=(100, 100), max_iter=100000))
    #     ),
    (
        'RandomForestRegressor',
        make_pipeline(ensemble.RandomForestRegressor())
    ),
    (
        'ensemble_GradientBoostingRegressor',
        make_pipeline(ensemble.GradientBoostingRegressor(**ensemble_params))
    ),
    (
        'XGBRegressor',
        make_pipeline(xgb.XGBRegressor(objective="reg:linear", random_state=42))
    ),
    (
        'PolynomialFeatures-2-RandomForestRegressor',
        make_pipeline(PolynomialFeatures(2), ensemble.RandomForestRegressor())
    ),
    (
        'PolynomialFeatures-2-ensemble_GradientBoostingRegressor',
        make_pipeline(PolynomialFeatures(2), ensemble.GradientBoostingRegressor(**ensemble_params))
    ),

    (
        'PolynomialFeatures-2-XGBRegressor',
        make_pipeline(PolynomialFeatures(2), xgb.XGBRegressor(objective="reg:linear", random_state=42))
    )
]


# clean up the dataframe where LUTs = 0
def predict(model, x):
    start = time.time()
    result = model.predict(x).astype(int).clip(min=0)
    end = time.time()
    # print("prediction takes " + str(end - start))
    return result


def get_shuffled_data(data_frame, X_column_names, Y_column_names, split_ratio=0.80):
    X = data_frame.loc[:, X_column_names]
    X = X.fillna(0)
    Y = data_frame.loc[:, Y_column_names]
    n_samples, n_features = X.shape
    _, n_targets = Y.shape
    n_split_index = int(split_ratio * n_samples)

    XX = np.array(X).reshape(n_samples, n_features)
    Y = np.array(Y).reshape(n_samples, n_targets)
    XY = np.concatenate([X, Y], axis=1)
    # clean up invalid entries
    #     invalid_rows = np.where(Y == 0)
    #     XY = np.delete(XY, invalid_rows, axis=0)

    np.random.shuffle(XY)

    train_fullname = XY[:n_split_index, 0]
    test_fullname = XY[n_split_index:, 0]
    train_set = XY[:n_split_index, 1:]
    train_set_X = train_set[:, :-1]
    train_set_Y = train_set[:, -1]
    test_set = XY[n_split_index:, 1:]
    test_set_X = test_set[:, :-1]
    test_set_Y = test_set[:, -1]

    return (train_fullname, test_fullname, train_set_X, train_set_Y, test_set_X, test_set_Y)


def get_metrics(gold, pred, names, fail_thresh=0.2):
    abs_error = np.abs(gold - pred).flatten()
    rel_error = abs_error  # if (gold == 0) else abs_error / gold; cannot think of a good way to do this...
    failed_idx = np.where(rel_error > fail_thresh)
    failed_node_names = names[failed_idx]
    failed_node_gold = gold[failed_idx]
    failed_node_pred = pred[failed_idx]

    return ReportStats(
        mse=mean_squared_error(gold, pred),
        mae=mean_absolute_error(gold, pred),
        r2=r2_score(gold, pred),
        abs_error=np.amax(abs_error),
        rel_error=np.amax(rel_error),
        failed_node_names=failed_node_names,
        failed_node_gold=failed_node_gold,
        failed_node_pred=failed_node_pred
    )


def eval_model(model_template, x_train, y_train, x_test, y_test, train_names, test_names):
    model = sklearn.base.clone(model_template)
    model.fit(x_train, y_train)
    y_test_pred = predict(model, x_test)
    y_train_pred = predict(model, x_train)

    train_stats = get_metrics(y_train, y_train_pred, train_names)
    test_stats = get_metrics(y_test, y_test_pred, test_names)
    return (train_stats, test_stats, model)


def test_and_store_pickled_model(df, sess, predict_type, model_name, pickled_model):
    test_model = pickle.loads(pickled_model)
    file_name = sess.node_type + "_" + predict_type + ".pkl"
    pickle.dump(test_model, open(file_name, "wb"))
    ff = df.loc[df.nodetype == sess.node_type]

    Y_column_names = [predict_type]
    X_column_names = sess.input_feature_list

    _, _, \
    x_test_0, y_test_0, \
    x_test_1, y_test_1 = \
        get_shuffled_data(
            ff, X_column_names, Y_column_names, split_ratio=0.80
        )

    # This is a rough check to estimate how well the model works on the full data set...
    # Cannot be taken as a valid statistical measure...
    # Would like to see what's the largest deviation...
    x_test = np.concatenate([x_test_0, x_test_1], axis=0)
    y_test = np.concatenate([y_test_0, y_test_1], axis=0)

    y_pred = test_model.predict(x_test)

    print("Testing the pickled model on " + predict_type + ". mae = ", mean_absolute_error(y_pred, y_test))


def retrain_models_on_session(df, sess):
    """
    Retrain a session specification with all the models in the model zoo.
    Report the one that achieves the lowest MAE.
    """
    ff = df.loc[df.nodetype == sess.node_type]

    def get_single_sess_regression(predict_type):
        Y_column_names = [predict_type]
        X_column_names = sess.input_feature_list
        train_full_name, test_full_name, \
        x_train, y_train, \
        x_test, y_test = \
            get_shuffled_data(
                ff, X_column_names, Y_column_names, split_ratio=0.80
            )
        print("Training Set contains ", x_train.shape[0], " points")
        print("Test Set contains ", x_test.shape[0], " points")

        stats = [
            (
                name,
                eval_model(
                    f, x_train, y_train, \
                    x_test, y_test, \
                    train_full_name, test_full_name
                )
            ) for (name, f) in model_zoo
        ]
        model_names = np.array([ts[0] for ts in stats])
        maes = np.array([ts[1][1].mae for ts in stats])
        model_copies = np.array([ts[1][2] for ts in stats])

        best_mae = np.amin(maes)
        best_mae_idx = np.argmin(maes)
        best_model_name = model_names[best_mae_idx]
        best_model_pickle = pickle.dumps(model_copies[best_mae_idx])

        selected_model = ModelMetaData(best_model_name, best_mae, best_model_pickle, predict_type)

        return selected_model

    best_models = [get_single_sess_regression(predict_type) for predict_type in XilinxFPGAResourceTargets]
    return best_models


FIFOSess = IRTrainingSession(
    "FIFONew",
    [
        "fullname",
        "mem_B0",  # not related?
        "mem_N0",
        "mem_a0",
        "mem_bitwidth",
        "mem_dim0",
        "mem_hist0muxwidth",
        "mem_hist0rlanes",
        "mem_hist0wlanes",
        "mem_nbufs",  # not related?
        "mem_p0"
    ]
)
# [1,1,1,1,1,8,1,1,1,1,1,1,1,1,1,32,2048,1,1,1,1,1,8,8,8,2,1,1,1,1,1,8,1,1,1,1]
SRAMSess = IRTrainingSession(
    "SRAMNew",
    [
        "fullname",
        "mem_B0",
        "mem_B1",
        "mem_B2",
        "mem_B3",
        "mem_B4",
        "mem_N0",
        "mem_N1",
        "mem_N2",
        "mem_N3",
        "mem_N4",
        "mem_a0",
        "mem_a1",
        "mem_a2",
        "mem_a3",
        "mem_a4",
        "mem_bitwidth",
        "mem_dim0",
        "mem_dim1",
        "mem_dim2",
        "mem_dim3",
        "mem_dim4",
        "mem_hist0muxwidth",
        "mem_hist0rlanes",
        "mem_hist0wlanes",
        "mem_hist1muxwidth",
        "mem_hist1rlanes",
        "mem_hist1wlanes",
        "mem_hist2muxwidth",
        "mem_hist2rlanes",
        "mem_hist2wlanes",
        "mem_nbufs",
        "mem_p0",
        "mem_p1",
        "mem_p2",
        "mem_p3",
        "mem_p4"
    ]
)

CounterChainSess = IRTrainingSession(
    "CounterChainNew",
    [
        "fullname",
        "cchain_ctr0par",
        "cchain_ctr0start",
        "cchain_ctr0step",
        "cchain_ctr0stop",
        "cchain_ctr1par",
        "cchain_ctr1start",
        "cchain_ctr1step",
        "cchain_ctr1stop",
        "cchain_ctr2par",
        "cchain_ctr2start",
        "cchain_ctr2step"
    ]
)

FixAddSess = IRTrainingSession(
    "FixAdd",
    [
        "fullname",
        "op_consta",
        "op_constb",
        "tp_dec",
        "tp_frac",
        "tp_sgn"
    ]
)

FixDivSess = IRTrainingSession(
    "FixDiv",
    [
        "fullname",
        "op_consta",
        "op_constb",
        "tp_dec",
        "tp_frac",
        "tp_sgn"
    ]
)

FixFMASess = IRTrainingSession(
    "FixFMA",
    [
        "fullname",
        "tp_dec",
        "tp_frac",
        "tp_sign"
    ]
)

FixMulSess = IRTrainingSession(
    "FixMul",
    [
        "op_consta",
        "op_constb",
        "tp_dec",
        "tp_frac",
        "tp_sign"
    ]
)

FixSubSess = IRTrainingSession(
    "FixSub",
    [
        "op_consta",
        "op_constb",
        "tp_dec",
        "tp_frac",
        "tp_sign"
    ]
)


FixToFixSess = IRTrainingSession(
    "FixToFix",
    [
        "tp_dec",
        "tp_frac",
        "tp_sgn",
        "tp_srcdec",
        "tp_srcfrac",
        "tp_srcsgn"
    ]
)


LIFONewSess = IRTrainingSession(
    "LIFONew",
    [
        "mem_B0",
        "mem_N0",
        "mem_a0",
        "mem_bitwidth",
        "mem_dim0",
        "mem_hist0muxwidth",
        "mem_hist0rlanes",
        "mem_hist0wlanes",
        "mem_nbufs",
        "mem_p0"

    ]
)

LineBufferNewSess = IRTrainingSession(
    "LineBufferNew",
    [
        "mem_B0",
        "mem_B1",
        "mem_N0",
        "mem_N1",
        "mem_a0",
        "mem_a1",
        "mem_bitwidth",
        "mem_dim0",
        "mem_dim1",
        "mem_hist0muxwidth",
        "mem_hist0rlanes",
        "mem_hist0wlanes",
        "mem_hist1muxwidth",
        "mem_hist1rlanes",
        "mem_hist1wlanes",
        "mem_nbufs",
        "mem_p0",
        "mem_p1"
    ]
)

ParallelPipeSess = IRTrainingSession(
    "ParallelPipe",
    [
        "ctrl_children",
        "ctrl_ii",
        "ctrl_lat",
        "ctrl_level",
        "ctrl_style"
    ]
)

RegFileNewSess = IRTrainingSession(
    "RegFileNew",
    [
        "mem_B0",
        "mem_B1",
        "mem_N0",
        "mem_N1",
        "mem_a0",
        "mem_a1",
        "mem_bitwidth",
        "mem_dim0",
        "mem_dim1",
        "mem_hist0muxwidth",
        "mem_hist0rlanes",
        "mem_hist0wlanes",
        "mem_hist1muxwidth",
        "mem_hist1rlanes",
        "mem_hist1wlanes",
        "mem_nbufs",
        "mem_p0",
        "mem_p1"
    ]
)

RegNewSess = IRTrainingSession(
    "RegNew",
    [
        "mem_bitwidth",
        "mem_nbufs"
    ]
)

StateMachineSess = IRTrainingSession(
    "StateMachine",
    [
        "ctrl_children",
        "ctrl_ii",
        "ctrl_lat",
        "ctrl_level",
        "ctrl_style"
    ]
)

UnbMulSess = IRTrainingSession(
    "UnbMul",
    [
        "op_consta",
        "tp_dec",
        "tp_frac",
        "tp_sgn"
    ]
)

UnbSatMulSess = IRTrainingSession(
    "UnbSatMul",
    [
        "tp_dec",
        "tp_frac",
        "tp_sgn"
    ]
)

UnitPipeSess = IRTrainingSession(
    "UnitPipe",
    [
        "ctrl_children",
        "ctrl_ii",
        "ctrl_lat",
        "ctrl_level",
        "ctrl_style"
    ]
)

UnrolledForeachSess = IRTrainingSession(
    "UnrolledForeach",
    [
        "ctrl_children",
        "ctrl_ii",
        "ctrl_lat",
        "ctrl_level",
        "ctrl_style"

    ]
)

UnrolledReduceSess = IRTrainingSession(
    "UnrolledReduce",
    [
        "ctrl_children",
        "ctrl_ii",
        "ctrl_lat",
        "ctrl_level",
        "ctrl_style"

    ]
)

sessions = [
    FIFOSess,
    SRAMSess,
    CounterChainSess,
    FixAddSess,
    FixDivSess,
    FixFMASess,
    FixMulSess,
    FixSubSess,
    FixToFixSess,
    LIFONewSess,
    LineBufferNewSess,
    ParallelPipeSess,
    RegFileNewSess,
    RegNewSess,
    StateMachineSess,
    UnbMulSess,
    UnbSatMulSess,
    UnitPipeSess,
    UnrolledForeachSess,
    UnrolledReduceSess
]

with open("test_mae.csv", "w+") as f:
    f.write(" ,")
    for rsc in XilinxFPGAResourceTargets:
        f.write(rsc + ", ")
    f.write("\n")

    for sess in sessions:
        f.write(sess.node_type + ", ")
        print(sess.node_type)
        model_metas = retrain_models_on_session(df, sess)
        for mm in model_metas:
            f.write(str(mm.best_mae) + ", ")
            # print(mm.best_mae, mm.best_model_name, mm.prediction_task)
        f.write("\n")

        for mm in model_metas:
            test_and_store_pickled_model(df, sess, mm.prediction_task, mm.best_model_name, mm.model_pickle)
