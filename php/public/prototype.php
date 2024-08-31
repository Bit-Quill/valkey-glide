#!/usr/bin/php                                                           
<?php                                                                    
use Amp\Future;
use function Amp\delay;
                                                                         
use Connection_request\ConnectionRequest;
use Connection_request\NodeAddress;

require __DIR__ . '/../vendor/autoload.php';                             
                                                                         
$header = file_get_contents('target/debug/glide_rs.h');
$ffi = FFI::cdef(                                                        
    $header,
    'target/debug/libglide_rs.dylib'
);

$path = $ffi->start_socket_listener();

$fp = fsockopen("unix://" . $path);

$host = "localhost";
$port = 6379;

$nodeAddr = new NodeAddress();
$nodeAddr->setHost($host);
$nodeAddr->setPort($port);

$connReq = new ConnectionRequest();
$connReq->setAddresses([$nodeAddr]);

$data = $connReq->serializeToString();

fwrite($fp, $data);


?>
