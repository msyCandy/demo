package com.example.demo.api;

import com.example.demo.MyUtils;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.admin.Admin;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Api
@Slf4j
@RestController
public class ApiController {
    private static String ip = "http://localhost:8545";
    Web3j web3j = Web3j.build(new HttpService(ip));
    Admin admin = Admin.build(new HttpService(ip));

    @ApiOperation("获取账户")
    @PostMapping("/getAccount")
    public String getAccount() throws IOException {
        List<String> accounts = web3j.ethAccounts().send().getAccounts();
        return new Gson().toJson(accounts);
    }

    @ApiOperation("新建账户")
    @PostMapping("/newAccount")
    public String newAccount(String password) throws IOException {
        String accountId = admin.personalNewAccount(password).send().getAccountId();
        log.info("新建账户【{}】，密码【{}】", accountId, password);
        return accountId;
    }

    @ApiOperation("解锁账户")
    @PostMapping("/unlockAccount")
    public String unlockAccount(String account, String password) throws IOException {
        return admin.personalUnlockAccount(account, password).send().accountUnlocked() ? "1" : "0";
    }

    @ApiOperation("获取区块高度")
    @PostMapping("/getBlockNumber")
    public String getBlockNumber() throws IOException {
        return web3j.ethBlockNumber().send().getBlockNumber().toString();
    }

    @ApiOperation("记录信息")
    @PostMapping("/mark")
    public String mark(String account, String password, String info) throws IOException {
        BigInteger nonce = getNonce(web3j, account);
        unlockAccount(account, password);
        String hash = web3j.ethSendTransaction(Transaction.createFunctionCallTransaction(account, nonce, new BigInteger("0"), null, account, new BigInteger("0"), "0x"+MyUtils.hexStringToString(info))).send().getTransactionHash();
        log.info("记录信息【{}】，账号【{}】，交易记录hash【{}】", info, account, hash);
        return hash;
    }

    @ApiOperation("获取信息")
    @PostMapping("/get")
    public String getTransaction(String hash) throws IOException {
        Optional<org.web3j.protocol.core.methods.response.Transaction> transaction = web3j.ethGetTransactionByHash(hash).send().getTransaction();
        return MyUtils.hexStringToString(transaction.get().getInput());
    }


    public static BigInteger getNonce(Web3j web3j, String addr) {
        try {
            EthGetTransactionCount getNonce = web3j.ethGetTransactionCount(addr, DefaultBlockParameterName.PENDING).send();
            if (getNonce == null) {
                throw new RuntimeException("net error");
            }
            return getNonce.getTransactionCount();
        } catch (IOException e) {
            throw new RuntimeException("net error");
        }
    }
}
