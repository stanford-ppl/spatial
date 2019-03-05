from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import tempfile

import pandas as pd
import six
import tensorflow as tf
import tensorflow_lattice as tfl
import timeit
import numpy as np

flags = tf.flags
FLAGS = flags.FLAGS

# Train example: python train.py --create_quantiles --output_dir=evaluations/output --quantiles_dir=evaluations/ --target=loadCycs --learning_rate=1 --train_epochs=100
# Test example: python train.py --output_dir=evaluations/output --quantiles_dir=evaluations/ --target=loadCycs

# test and train data set paths
project_dir = "./data/"
flags.DEFINE_string("train", project_dir + "train", "Path to test file.")
flags.DEFINE_string("test", project_dir + "test", "Path to train file.")

# Run mode of the program.
flags.DEFINE_string(
    "run", "train", "One of 'train', 'evaluate', 'time' or 'save', train will "
    "train on training data and also optionally evaluate; evaluate will "
    "evaluate train and test data; save saves the trained model so far "
    "so it can be used by TensorFlow Serving.")

# Model flags.
flags.DEFINE_string(
    "output_dir", "./output",
    "Directory where to store the model. If not set a temporary directory "
    "will be automatically created.")
flags.DEFINE_string(
    "model_type", "calibrated_lattice",
    "Types defined in this example: calibrated_linear, calibrated_lattice, "
    " calibrated_lattice, calibrated_etl, calibrated_dnn")
flags.DEFINE_integer("batch_size", 100,
                     "Number of examples to include in one batch. Increase "
                     "this number to improve parallelism, at cost of memory.")
flags.DEFINE_string("hparams", None,
                    "Model hyperparameters, see hyper-parameters in Tensorflow "
                    "Lattice documentation. Example: --hparams=learning_rate="
                    "0.1,lattice_size=2,num_keypoints=100")


# Calibration quantiles flags.
flags.DEFINE_bool("create_quantiles", False,
                  "Run once to create histogram of features for calibration. "
                  "It will use the --train dataset for that.")
flags.DEFINE_string(
    "quantiles_dir", "./",
    "Directory where to store quantile information, defaults to the model "
    "directory (set by --output-dir) but since quantiles can be reused by "
    "models with different parameters, you may want to have a separate "
    "directory.")

# Training flags.
flags.DEFINE_integer("train_epochs", 50,
                     "How many epochs over data during training.")
flags.DEFINE_bool(
    "train_evaluate_on_train", True,
    "If set, every 1/10th of the train_epochs runs an evaluation on the "
    "full train data.")
flags.DEFINE_bool(
    "train_evaluate_on_test", False,
    "If set, every 1/10th of the train_epochs runs an evaluation on the "
    "full test data.")


# Extra flags: 
#   label defaults to normal (i.e. a non-malicious packet)
#   rtl_seed is a random seed to intialize lattices   
flags.DEFINE_string("target", "loadCycs", "")
flags.DEFINE_integer("rtl_seed", 337893, "")
flags.DEFINE_integer("num_lattices", 1, "")
flags.DEFINE_float("learning_rate", 20.0, "")

CSV_COLUMNS = ['loads',
               'stores',
               'gateds',
               'outerIters',
               'innerIters',
               # 'bitwidth',
               # 'innerPar',
               'loadCycs',
               'storeCycs',
               'gatedCycs']

competitors = ['loads', 'stores', 'gateds']
payloads = ['outerIters', 'innerIters']

targets = {"loadCycs", "storeCycs", "gatedCycs"}

# Set up test and train functions with relevant parameters
def get_test_input_fn(batch_size, num_epochs, shuffle):
  return get_input_fn(FLAGS.test, batch_size, num_epochs, shuffle)

def get_train_input_fn(batch_size, num_epochs, shuffle):
  return get_input_fn(FLAGS.train, batch_size, num_epochs, shuffle)


# Copy of data read from train/test files: keep copy to avoid re-reading
# it at every training/evaluation loop.
_df_data = {}
_df_data_labels = {}



# Load data from files
def get_input_fn(file_path, batch_size, num_epochs, shuffle):
  """Returns an input_fn closure for given parameters."""
  if file_path not in _df_data:

    # Load data in CSV_COLUMNS format
    print("Loading data from ", file_path)
    _df_data[file_path] = pd.read_csv(
        tf.gfile.Open(file_path),
        sep='\t',
        names=CSV_COLUMNS,
        skipinitialspace=True,
        engine="python",
        dtype=np.float32)
    _df_data[file_path].drop(columns=list(targets - {FLAGS.target}), errors="ignore", inplace=True)

    # Mark labels
    _df_data_labels[file_path] = (_df_data[file_path][FLAGS.target])

  # set up data with labels
  return tf.estimator.inputs.pandas_input_fn(
      x=_df_data[file_path],
      y=_df_data_labels[file_path],
      batch_size=batch_size,
      shuffle=shuffle,
      num_epochs=num_epochs,
      num_threads=1)


# Create feature columns with correct categorical vocabularies
def create_feature_columns():
  return [tf.feature_column.numeric_column(feat) for feat in CSV_COLUMNS if feat not in targets]

# Create quantiles based on batch size
def create_quantiles(quantiles_dir):
  """Creates quantiles directory if it doesn't yet exist."""
  batch_size = 50
  input_fn = get_train_input_fn(
      batch_size=batch_size, num_epochs=1, shuffle=False)
  # Reads until input is exhausted, 50 at a time.
  tfl.save_quantiles_for_keypoints(
      input_fn=input_fn,
      save_dir=quantiles_dir,
      feature_columns=create_feature_columns(),
      num_steps=None)


# Print hyper parameters
def _pprint_hparams(hparams):
  """Pretty-print hparams."""
  print("* hparams=[")
  for (key, value) in sorted(six.iteritems(hparams.values())):
    print("\t{}={}".format(key, value))
  print("]")


# # Create network
# def create_network(model_fn1, model_fn2, feature_columns, config, quantiles_dir):
#   """Creates a calibrated Lattice estimator."""
#   """ Lattice 0 (Competitors) """
#   feature_names = [fc.name for fc in feature_columns if fc not in payloads]
#   hparams = tfl.CalibratedLatticeHParams(
#       feature_names=feature_names,
#       learning_rate=FLAGS.learning_rate,
#       lattice_l2_laplacian_reg=5.0e-4,
#       # lattice_l2_torsion_reg=1.0e-4,
#       interpolation_type='hypercube',
#       num_keypoints=8,
#       lattice_size=5,
#       lattice_rank=len(feature_names),
#       num_lattices=1,
#       optimizer=tf.train.AdamOptimizer)
#   hparams.parse(FLAGS.hparams)
#   _pprint_hparams(hparams)
#   (layer00, _, _, _) = tfl.calibrated_lattice_regressor(
#     model_fn1,
#     feature_columns=feature_columns,
#     model_dir=config.model_dir,
#     config=config,
#     quantiles_dir=quantiles_dir,
#     keypoints_initializers_fn=None,
#     optimizer=tf.train.AdamOptimizer,
#     hparams=hparams
#   )

#   """ Lattice 1 (Transfer info) """
#   feature_names = [fc.name for fc in feature_columns if fc not in competitors]
#   hparams = tfl.CalibratedLatticeHParams(
#       feature_names=feature_names,
#       learning_rate=FLAGS.learning_rate,
#       lattice_l2_laplacian_reg=5.0e-4,
#       # lattice_l2_torsion_reg=1.0e-4,
#       interpolation_type='hypercube',
#       num_keypoints=8,
#       lattice_size=16,
#       lattice_rank=len(feature_names),
#       num_lattices=1,
#       optimizer=tf.train.AdamOptimizer)
#   hparams.parse(FLAGS.hparams)
#   _pprint_hparams(hparams)
#   (layer01, _, _, _) = tfl.calibrated_lattice_regressor(
#     model_fn2,
#     feature_columns=feature_columns,
#     model_dir=config.model_dir,
#     config=config,
#     quantiles_dir=quantiles_dir,
#     keypoints_initializers_fn=None,
#     optimizer=tf.train.AdamOptimizer,
#     hparams=hparams
#   )

#   """ Lattice 2 (combine lattices 0 and 1) """
#   feature_names = ['lattice0','lattice1']
#   hparams = tfl.CalibratedLatticeHParams(
#       feature_names=feature_names,
#       learning_rate=FLAGS.learning_rate,
#       lattice_l2_laplacian_reg=5.0e-4,
#       # lattice_l2_torsion_reg=1.0e-4,
#       interpolation_type='hypercube',
#       num_keypoints=8,
#       lattice_size=16,
#       lattice_rank=len(feature_names),
#       num_lattices=1,
#       optimizer=tf.train.AdamOptimizer)
#   hparams.parse(FLAGS.hparams)
#   _pprint_hparams(hparams)
#   layer10 = tfl.calibrated_lattice_regressor(
#     feature_columns=feature_columns,
#     model_dir=config.model_dir,
#     config=config,
#     quantiles_dir=quantiles_dir,
#     keypoints_initializers_fn=None,
#     optimizer=tf.train.AdamOptimizer,
#     hparams=hparams
#   )

#   layer10


# Create a set of randomly initialized lattices with calibrator inputs
def create_calibrated_lattice(feature_columns, config, quantiles_dir):
  """Creates a calibrated Lattice estimator."""
  feature_names = [fc.name for fc in feature_columns]
  hparams = tfl.CalibratedLatticeHParams(
      feature_names=feature_names,
      learning_rate=FLAGS.learning_rate,
      lattice_l2_laplacian_reg=5.0e-4,
      # lattice_l2_torsion_reg=1.0e-4,
      interpolation_type='hypercube',
      num_keypoints=8,
      feature__outerIters__monotonicity=1,
      feature__innerIters__monotonicity=1,
      # feature__innerIters__lattice_size=3,
      # feature__outerIters__lattice_size=3,
      # monotonicity=frozenset({'outerIters': 1}.items),
      lattice_size=3,
      lattice_rank=5,
      num_lattices=FLAGS.num_lattices,
      optimizer=tf.train.AdamOptimizer)

  # Specific feature parameters.
  hparams.parse(FLAGS.hparams)
  _pprint_hparams(hparams)

  return tfl.calibrated_lattice_regressor(
    feature_columns=feature_columns,
    model_dir=config.model_dir,
    config=config,
    quantiles_dir=quantiles_dir,
    keypoints_initializers_fn=None,
    optimizer=tf.train.AdamOptimizer,
    hparams=hparams
  )





# Create an estimator
# TODO - Add other lattice models here
def create_estimator(config, quantiles_dir):
  """Creates estimator for given configuration based on --model_type."""
  feature_columns = create_feature_columns()
  #FLAGS.model_type == "calibrated_rtl":
  return create_calibrated_lattice(feature_columns, config, quantiles_dir)

  #raise ValueError("Unknown model_type={}".format(FLAGS.model_type))



# Evaluator that keeps track accuracy and loss
def evaluate_on_data(estimator, data):
  """Evaluates and prints results, set data to FLAGS.test or FLAGS.train."""
  name = os.path.basename(data)
  evaluation = estimator.evaluate(
      input_fn=get_input_fn(
          file_path=data,
          batch_size=FLAGS.batch_size,
          num_epochs=1,
          shuffle=False),
      name=name)


  metrics = [
          "average_loss"
          ]
  print(estimator)
  print(evaluation)
  metric_string = "\t".join("{}={:.8f}".format(metric, evaluation[metric]) for metric in metrics)
  print(metric_string)

  return evaluation
  #print("  Evaluation on '{}':\taccuracy={:.4f}\taverage_loss={:.4f}".format(
      #name, evaluation["accuracy"], evaluation["average_loss"]))

# Training function
def train(estimator):
  """Trains estimator and optionally intermediary evaluations."""
  if not FLAGS.train_evaluate_on_train and not FLAGS.train_evaluate_on_test:
    estimator.train(input_fn=get_train_input_fn(
        batch_size=FLAGS.batch_size,
        num_epochs=FLAGS.train_epochs,
        shuffle=True))
  else:
    # Train 1/10th of the epochs requested per loop, but at least 1 per loop.
    epochs_trained = 0
    loops = 0
    while epochs_trained < FLAGS.train_epochs:
      loops += 1
      next_epochs_trained = int(loops * FLAGS.train_epochs / 10.0)
      epochs = max(1, next_epochs_trained - epochs_trained)
      epochs_trained += epochs
      inp = get_train_input_fn(batch_size=FLAGS.batch_size, num_epochs=epochs, shuffle=True)
      estimator.train(input_fn=inp)
      print("Trained for {} epochs, total so far {}:".format(
          epochs, epochs_trained))
      if FLAGS.train_evaluate_on_train:
          evaluate_on_data(estimator, FLAGS.train)
      if FLAGS.train_evaluate_on_test:
          evaluate_on_data(estimator, FLAGS.test)


# Train before testing
def evaluate(estimator):
  """Runs straight evaluation on a currently trained model."""
  evaluate_on_data(estimator, FLAGS.train)
  evaluate_on_data(estimator, FLAGS.test)

def timed_evaluate(estimator):

  def eval_func():
    evaluate_on_data(estimator, FLAGS.test)


  """ Timed evaluation over 50 trials"""
  runtime = timeit.timeit(eval_func, number = 3)*1000000
  sperinf = (runtime / float(10000))
  print("%f us/inference (%fus / %d)" % (sperinf, runtime, 10000))

# Main function that sets up and runs program
def main(args):
  del args  # Not used.

  # Prepare directories.
  output_dir = FLAGS.output_dir
  if output_dir is None:
    output_dir = tempfile.mkdtemp()
    tf.logging.warning("Using temporary folder as model directory: %s",
                       output_dir)
  quantiles_dir = FLAGS.quantiles_dir or output_dir

  # Create quantiles if required.
  if FLAGS.create_quantiles:
    if FLAGS.run != "train":
      raise ValueError(
          "Can not create_quantiles for mode --run='{}'".format(FLAGS.run))
    create_quantiles(quantiles_dir)

  # Create config and then model.
  config = tf.estimator.RunConfig().replace(model_dir=output_dir)
  estimator = create_estimator(config, quantiles_dir)

  if FLAGS.run == "train":
    train(estimator)

  elif FLAGS.run == "evaluate":
    evaluate(estimator)
    
  elif FLAGS.run == "time":
    timed_evaluate(estimator)

  else:
    raise ValueError("Unknown --run={}".format(FLAGS.run))


if __name__ == "__main__":
  tf.app.run()
