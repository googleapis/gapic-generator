<?php
require 'vendor/autoload.php';

use Google\Cloud\PubSub\V1\PublisherClient;
use Google\Pubsub\V1\GetTopicRequest;
use Google\Pubsub\V1\PublisherGrpcClient;

function read_cert($certfile)
{
  $file = fopen($certfile, "r") or die ("cannot open file");
  $content = fread($file, filesize($certfile));
  fclose($file);
  return $content;
}

function grpc_get($host, $port, $creds)
{
  $client = new PublisherGrpcClient(sprintf("%s:%d", $host, $port), [
    'credentials' => $creds,
  ]);
  $meta = array('x-goog-api-client' => ['gl-php/5.5.9-1ubuntu4.21 gapic/0.5.1 gax/0.9.0 grpc/0.1.0']);

  return function($top) use($client, $meta) {
    $req = new GetTopicRequest();
    $req->setTopic($top);
    list($resp, $status) = $client->getTopic($req, $meta)->wait();
    return $resp;
  };
}

function gapic_get($host, $port, $creds)
{
  $client = new PublisherClient([
    'serviceAddress' => $host,
    'port' => $port,
    'sslCreds' => $creds,
  ]);
  return function($top) use($client) {
    return $client->getTopic($top);
  };
}

$options = getopt('', array('host:', 'port:', 'cert:', 'client:', 'warmup_time:', 'run_time:'));
$host = isset($options['host']) ? $options['host'] : 'localhost';
$port = intval(isset($options['port']) ? $options['port'] : '8080');
$creds = Grpc\ChannelCredentials::createSsl(read_cert($options['cert']));

$client = isset($options['client']) ? $options['client'] : '';
if ($client == 'grpc') {
  $getfn = 'grpc_get';
} elseif ($client == 'gapic') {
  $getfn = 'gapic_get';
} else {
  die("unknown client: $client");
}

$fn = $getfn($host, $port, $creds);

$warmtime = intval(isset($options['warmup_time']) ? $options['warmup_time'] : '60');
$runtime = intval(isset($options['run_time']) ? $options['run_time'] : '60');

const TOPIC = "projects/benchmark-project/topics/benchmark-topic";

$start = microtime(true);
$warmend = $start + $warmtime;
$runend = $warmend + $runtime;

while (microtime(true) < $warmend) {
  $fn(TOPIC);
}
$start = microtime(true);
$reqnum = 0;
$errnum = 0;
while (microtime(true) < $runend) {
  $resp = $fn(TOPIC);
  if ($resp->getName() != TOPIC) {
    $errnum++;
  }
  $reqnum++;
}
$dur = microtime(true) - $start;
printf("time: %f sec, calls: %d, avg latency: %f usec\n", $dur, $reqnum, $dur/$reqnum*1000*1000);
